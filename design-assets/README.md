# RoamMate pixel koala artwork

RoamMate ships one production companion: Buddy, a desktop-pet-style pixel koala designed for the
app's teal/coral visual system. Every visible garment is baked into every animation frame so it
stays fitted while Buddy walks, curls up, bounces, and reacts to touch.

## Outfit catalogue

The nine production outfits are split into two groups:

- **Automatic weather gear:** `everyday`, `sunshine`, `raincoat`, `windbreaker`, and `winter`.
  Weather selects one of these while the wardrobe is in its default **Weather** mode.
- **Manual fashion:** `explorer`, `streetwear`, `festival`, and `pajamas`. Swiping or using the
  wardrobe controls selects these looks without changing Buddy's species or animation behaviour.

The weather background and weather-to-outfit rules are rendered in Compose. The artwork in this
folder supplies only Buddy and the fitted clothes, which keeps the same sprites reusable across
sunny, cloudy, rainy, stormy, foggy, and snowy scenes.

## Animation contract

Every outfit sheet is exactly **4 columns × 6 rows** (24 cells). Each cell is 128 × 128 pixels;
the normalized sheet is therefore 512 × 768 pixels. Rows must stay in this order:

1. `idle` — breathing and ear motion, including a natural blink in frames 3–4
2. `walk` — a seamless in-place koala walk
3. `sleep` — a continuous curled-up breathing loop
4. `happy` — a continuous bounce loop
5. `sad` — lowered posture with slow breathing
6. `petted` — eyes-close and ear-squash reaction

All six rows contain exactly four frames. The Android build therefore has
**9 outfits × 6 loops × 4 frames = 216** production koala cells. Files use the deterministic name
`pet_pixel_koala_<outfit>_<loop>_<frame>.webp`, where `<frame>` is `0` through `3`. They are stored
as lossless transparent WebP in `app/src/main/res/drawable-nodpi/` and rendered with
nearest-neighbour sampling so the pixels remain crisp.

Older corgi, penguin, and kangaroo sheets are retained as design prototypes only; the current app
does not expose them as selectable companions.

## Normalizing and slicing a sheet

Run `slice_pet_sheet.py` from the repository root after creating or editing a 4 × 6 source sheet.
For example:

```bash
python design-assets/slice_pet_sheet.py path/to/koala_streetwear_source.png streetwear \
  --sheet-output design-assets/pet-pixel-sheets/koala_streetwear_sheet.webp \
  --frames-output app/src/main/res/drawable-nodpi
```

The script converts the source to RGBA, normalizes it to 512 × 768 with nearest-neighbour
resampling, stores the lossless catalogue sheet, and exports all 24 Android frames. The lowercase
outfit argument must match the corresponding `PetOutfit` enum name. After slicing, run the unit
tests; `PetFrameResourcesTest` verifies the complete deterministic name matrix without requiring
Android resource lookup.

## Base generation prompt

> Create one production-ready transparent PNG sprite sheet for an Android virtual travel pet in
> crisp hand-crafted desktop-pet pixel art. Use exactly 4 equal columns and 6 equal rows with no
> grid, text, border, scenery, floor or shadow. Keep the same koala, anchor and scale in every cell.
> Rows are seamless four-frame loops: idle breathing with a blink, in-place walk, sleeping
> breathing, happy bounce, sad breathing, and happily petted. Use a dark navy pixel outline and
> restrained RoamMate teal #009C9F and coral #FF6F61 accents.

## Outfit edit prompt

> Edit the supplied 4 × 6 pixel-art sheet while preserving identity, koala anatomy, all 24 poses,
> expressions, anchor, scale and transparent canvas. Add the specified garment to every frame.
> Make it genuinely fitted and pose-aware for idle, walking, curled sleep, happy, sad and petted
> rows; fabric must bend and compress with the body, never cover the eyes, float, or look pasted on.
> Keep crisp hard pixel edges and output a transparent PNG only.
