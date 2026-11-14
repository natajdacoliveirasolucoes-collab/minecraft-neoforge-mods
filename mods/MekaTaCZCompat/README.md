# MekaSuit / TaCZ first-person arm compatibility

Client-only compatibility mod for Minecraft 1.21.1 / NeoForge 21.1.265,
Mekanism 10.7.19.85 and TaCZ 1.1.8.

Mekanism's `RenderTickHandler.renderArm` resets the player arm pose, draws its
MekaSuit model, then cancels the first-person arm event. This can overlap TaCZ's
gun pose. This patch returns from that listener before its pose mutations only
when the player wears a MekaSuit chestplate and holds a TaCZ gun in the main hand.
It leaves the event itself untouched so the regular gun/hand renderer continues.

Install `MekaTaCZCompat-1.0.0.jar` in the client's `mods` folder and restart the
client. Keep the original Mekanism and TaCZ JARs. The server does not need this mod.
No equipment, energy, damage, animations, recipes, networking, or third-person
armor rendering is modified.

Client commands (no OP required, not sent to the server):

- `/mekataczcompat` — current state.
- `/mekataczcompat enabled false` — original rendering for comparison.
- `/mekataczcompat enabled true` — enable the fix again.

The toggle is saved in `config/mekataczcompat-client.toml` and takes effect
immediately. Compare hip fire, aiming, reload, sprinting and both dominant hands;
switch to a non-gun item and verify the MekaSuit arm still appears. Check the
third-person armor too. A one-time log message confirms the guarded branch ran.

Visual validation in the user's client is required; compilation alone does not
prove that every gun animation is correct.

## Validation

An isolated NeoForge client completed resource loading and confirmed injection into the actual Mekanism arm listener. The player accepted the visual result. Check hip fire, aim, reload and non-gun items on your own client; not every weapon animation has an automated visual assertion.

The compatibility mod keeps the upstream Mekanism and TaCZ jars unchanged.
Credit: Patch local por Natã Oliveira (nata).
