# RoamMate companion artwork

`pet_companions_source.png` is the transparent source sheet created for the RoamMate pet feature.
The four production crops are normalized to the same 512 × 512 transparent canvas in
`app/src/main/res/drawable-nodpi/` so every screen can swap characters without layout jumps.

## Generation brief

> Create a polished 2 × 2 character sprite sheet for a modern Australian travel mobile app. Show
> exactly four separate full-body 2D cartoon mascots: a cheerful corgi puppy, a friendly koala, a
> cute penguin, and a cheerful kangaroo. Use one consistent soft rounded vector-like style, clean
> dark outlines, warm expressive faces, subtle shading, and small coordinated teal travel
> accessories. Center one character in each equal quadrant, keep generous clear space around every
> silhouette, do not let characters overlap or cross quadrant boundaries, and use a genuinely
> transparent background. No text, labels, scenery, borders, tiles, shadows outside the characters,
> or extra objects. High-resolution game/app asset quality.

## Motion set

`pet-motion/` contains five transparent 4 × 3 source sheets for each companion: everyday,
sunshine, raincoat, windbreaker and winter. Every sheet was generated for that exact mascot and
outfit, and uses the same row-major pose order:

1. idle, blink, curious, inspect
2. walk A, walk B, happy, sad
3. yawn, sleep, petted, surprised

The Android resources are individual 362 × 362 transparent WebP crops named
`pet_<companion>_<outfit>_<pose>.webp` (the everyday set omits the outfit segment).
`PetBehaviorEngine` gives every animal a different autonomous action pool and interaction
vocabulary; `InteractivePetStage` interrupts that loop for head/body taps, double taps, holds,
dragging, treats and shake-sensor reactions, then returns the pet to autonomous life.

Weather-specific gear is baked into every pose instead of being drawn as a generic UI overlay.
Glasses, raincoats, windbreakers, hats, coats and scarves therefore follow each animal's body,
including walking, curled sleeping, jumping and petted poses. `PetFrameResources.kt` explicitly
maps all 240 production frames so Android resource shrinking cannot remove them.

### Motion generation prompt

> Create a clean 4 columns by 3 rows transparent sprite sheet of the exact same travel companion
> from the supplied reference. Show twelve centered full-body poses in row-major order: neutral
> idle, blink, curious head tilt, inspecting the ground or signature prop, two walking steps, joyful
> jump, sad pose, sleepy yawn, curled sleeping, enjoying a head pat, and surprised photo pose.
> Preserve identity, proportions, travel accessories, rendering style and colors. Keep consistent
> scale and transparent padding. No grid, text, labels, numbers, watermark, overlap or cropping.

### Weather-outfit generation prompt

> Recreate this exact 4 × 3 companion motion sheet with the same twelve poses, grid positions,
> character identity, scale, line work and transparent padding. Add one weather outfit designed
> specifically for the animal's anatomy: sunshine visor/sunglasses, fitted yellow raincoat,
> fitted turquoise windbreaker, or fitted coral winter coat with hat/scarf. The outfit must wrap
> naturally around the body in every pose, including walking, jumping, sleeping and being petted;
> do not paste a floating overlay. Transparent background, no text, grid or extra characters.
