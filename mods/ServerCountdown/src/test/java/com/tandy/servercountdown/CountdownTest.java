package com.tandy.servercountdown;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CountdownTest {
    @TempDir Path dir;
    final Instant now = Instant.parse("2026-11-06T02:00:00Z");

    @Test void parsesManausAndRejectsInvalidDatesAndAmbiguousTimes() {
        assertEquals(now, CountdownState.parseDeadline("2026-11-05", "22:00", ZoneId.of("America/Manaus")));
        assertThrows(DateTimeException.class, () -> CountdownState.parseDeadline("2026-02-30", "22:00", ZoneId.of("UTC")));
        assertThrows(DateTimeException.class, () -> CountdownState.parseDeadline("2026-11-05", "25:00", ZoneId.of("UTC")));
        assertThrows(IllegalArgumentException.class, () -> CountdownState.parseDeadline("2026-11-01", "01:30", ZoneId.of("America/New_York")));
        assertThrows(IllegalArgumentException.class, () -> CountdownState.parseDeadline("2026-03-08", "02:30", ZoneId.of("America/New_York")));
    }
    @Test void timeUsesRealDeadlineRatherThanServerTicksAndNeverGoesNegative() {
        var state = CountdownState.defaults().start("Evento", now, now.plusSeconds(90061), ZoneId.of("UTC"));
        assertEquals("1d 01:01:01", state.remaining(now));
        assertEquals("00:00:01", state.remaining(state.deadline().minusMillis(1)));
        assertEquals("00:00:00", state.remaining(state.deadline().plusSeconds(99)));
        assertEquals(1, state.progress(now));
        assertEquals(.5f, state.progress(now.plusMillis(45030500)), .0001f);
        assertFalse(state.active(state.deadline()));
        assertEquals(0, state.progress(state.deadline()));
        assertEquals(1, state.progress(now.minusSeconds(3600)));
    }
    @Test void stoppingAndReschedulingKeepIndividualPreferences() {
        UUID a=UUID.randomUUID(), b=UUID.randomUUID();
        var state=CountdownState.defaults().visibility(a,false).visibility(b,false).visibility(a,true)
            .start("Evento",now,now.plusSeconds(60),ZoneId.of("America/Manaus"));
        assertEquals(Set.of(b),state.hiddenPlayers());
        assertEquals(Set.of(b),state.stop().hiddenPlayers());
        assertFalse(state.stop().active(now));
        assertThrows(UnsupportedOperationException.class,()->state.hiddenPlayers().clear());
        assertThrows(IllegalArgumentException.class,()->state.start("Evento",now,now.minusSeconds(1),ZoneId.of("UTC")));
        assertThrows(IllegalArgumentException.class,()->state.start("\n",now,now.plusSeconds(1),ZoneId.of("UTC")));
    }
    @Test void savesDefaultsAndPersistsAbsoluteDeadlineAndUuidPreferenceAcrossReload() throws Exception {
        var store=new CountdownStore(dir.resolve("config.json"));
        assertEquals(CountdownState.defaults(),store.load());
        UUID player=UUID.randomUUID();
        var state=store.load().start("Contagem <teste>",now,now.plusSeconds(3600),ZoneId.of("America/Manaus")).visibility(player,false);
        store.save(state);
        assertEquals(state,new CountdownStore(dir.resolve("config.json")).load());
        assertEquals("00:30:00",store.load().remaining(now.plusSeconds(1800)));
        assertTrue(Files.readString(dir.resolve("config.json")).contains("<teste>"));
        try(var files=Files.list(dir)) { assertEquals(1,files.count()); }
    }
    @Test void invalidConfigIsNeverReplacedWithDefaults() throws Exception {
        Path path=dir.resolve("config.json");
        for(String data:List.of("{broken", "{}", "{\"schemaVersion\":99}")) {
            Files.writeString(path,data);
            assertThrows(java.io.IOException.class,()->new CountdownStore(path).load());
            assertEquals(data,Files.readString(path));
        }
    }
    @Test void scheduleNeedsOnlyDateTimeAndTitleAndAlwaysUsesManaus() {
        var state=CountdownState.defaults().schedule("2026-11-06 20:00 Início do evento", now);
        assertEquals(Instant.parse("2026-11-07T00:00:00Z"),state.deadline());
        assertEquals(ZoneId.of("America/Manaus"),state.timezone());
        assertEquals("Início do evento",state.title());
        assertEquals("Início do evento - 22:00:00 (GMT-4)",state.bossbarTitle(now));
        assertThrows(IllegalArgumentException.class,()->state.schedule("2026-11-06 20:00",now));
        assertThrows(IllegalArgumentException.class,()->state.schedule("2020-01-01 20:00 Passado",now));
        assertThrows(DateTimeException.class,()->state.schedule("2026-02-30 20:00 Data inválida",now));
    }
    @Test void existingSavedScheduleKeepsDeadlineAndVisibilityAndCanShowHalfProgress() throws Exception {
        var store=new CountdownStore(dir.resolve("config.json"));
        UUID hidden=UUID.randomUUID();
        Instant target=now.plusSeconds(3600);
        // The previous version's schema and timezone field still load unchanged.
        var previous=CountdownState.defaults().start("Evento",now.minusSeconds(3600),target,ZoneId.of("UTC")).visibility(hidden,false);
        store.save(previous);
        var restored=store.load();
        assertEquals(target,restored.deadline());
        assertEquals(Set.of(hidden),restored.hiddenPlayers());
        assertEquals(.5f,restored.progress(now));
        assertEquals("Evento - 01:00:00 (GMT-4)",restored.bossbarTitle(now));
    }
}
