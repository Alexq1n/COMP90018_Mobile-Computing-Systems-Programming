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

Weather-specific gear is deliberately drawn as a separate Compose layer in `PetAvatar.kt`. This
keeps rain, sun, wind, and winter outfits consistent across all four base characters and makes
future art replacement independent of the weather logic.
