# RoamMate pixel companion artwork

The production companions are compact desktop-pet-style pixel characters designed for RoamMate's
teal/coral UI. `pet-pixel-sheets/` contains one 4 × 6 transparent source sheet for every animal and
weather outfit (4 animals × 5 outfits = 20 sheets).

Each sheet has four frames per row:

1. Idle — breathing and tail/ear motion, with a natural blink in frames 3–4
2. Walk — animal-specific paws, waddle, slow koala steps, or kangaroo hops
3. Sleep — a continuous breathing loop
4. Happy — a continuous bounce/wag loop
5. Sad — lowered posture with slow breathing
6. Petted — eyes-close, ear-squash and tail/flipper reaction

The Android build uses 480 lossless transparent WebP cells in
`app/src/main/res/drawable-nodpi/`, named
`pet_pixel_<animal>_<outfit>_<loop>_<frame>.webp`. Each cell is 128 × 128 and is rendered with
nearest-neighbour sampling to keep the pixels crisp.

## Behaviour model

`PetBehaviorEngine.restingAction()` selects one durable state from the current pet mood: Ready uses
Idle, Excited uses Happy, Cozy uses Sleep, and Tired uses Sad. The pet does not randomly jump among
those states. `PetAvatar` loops the four authored frames inside that state indefinitely. A real
interaction (tap, hold, drag, shake or treat) temporarily selects another loop and then returns to
the mood's durable state.

Weather gear is baked into every animation frame rather than stretched over the character. Each
animal has anatomy-specific everyday, sunshine, raincoat, windbreaker and winter artwork, including
pose-aware clothing for walking, curling up, bouncing and receiving a pat.

## Base generation prompt

> Create one production-ready transparent PNG sprite sheet for an Android virtual travel pet in
> crisp hand-crafted desktop-pet pixel art. Use exactly 4 equal columns and 6 equal rows with no
> grid, text, border, scenery, floor or shadow. Keep the same character, anchor and scale in every
> cell. Rows are seamless four-frame loops: idle breathing with a blink, in-place walk, sleeping
> breathing, happy bounce, sad breathing, and happily petted. Use a dark navy pixel outline and
> restrained RoamMate teal #009C9F and coral #FF6F61 accents.

## Outfit edit prompt

> Edit the supplied 4 × 6 pixel-art sheet while preserving identity, species anatomy, all 24
> poses, expressions, anchor, scale and transparent canvas. Add the specified weather garment to
> every frame. Make it genuinely fitted and pose-aware for idle, walking, curled sleep, happy, sad
> and petted rows; fabric must bend and compress with the body, never cover the eyes, float, or look
> pasted on. Keep crisp hard pixel edges and output a transparent PNG only.
