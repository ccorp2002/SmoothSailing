# SmoothSailing

Inspired by the legacy of `Movecraft` (By `Airship-Pirates/APDev`), and borrowing code, and project layout from an early `BlockShips` version (By user `def9a2a4`, on GitHub), outside of these two references, this largely is an independant, solo, project.



## IMPORTANT NOTICE -(From C_Corp2002)

> I may not update this Repo for (potentially) long streches of time, as this is my hobby project and primarily designed for my up-and-coming (also hobby) server(s).

> (This is also my first major project to be put into the open source developer world.)
 
> If you can't, or won't consider I have other things - that I want to, or have to do - I may refuse to provide support.
> *Let this be your warning.*


## Dependancies & Other Info


> **Hard Dependancies:** `PaperMC 1.21.9+`

> **Soft Dependancies:** `Towny, WorldGuard, Cannons`

> **Controls:**
```
| Key    | Action                    |
| ------ | ------------------------- |
| W      | Move forward              |
| S      | Move backward / brake     |
| A      | Rotate left               |
| D      | Rotate right              |
| Space  | Ascend (Flying vehicles)  |
| Sprint | Descend (flying vehicles) |
```

`(Similar to BlockShips; with alternative, Movecraft-like control-schemes coming later.)`


### `blocks.yml` Heading:
```yml
## BLOCK ENTRY-FORMATTING INFO:
# block_name: #(Or "#block_tag", "*_block" <-(Wildcard))
#   allowed: true/false # (Boolean - Optional; Defaults to `false` if not provided)
#                # Any block that is intended to be apart of any ship class, must be specified here.
#
#   seat: true/false # (Boolean - Optional; Defaults to `false` if not provided)
#                # Allows an entity to be mounted to the block as to not get possibly crushed by the ship while it moves.
#                # Players may also sit on these blocks, if the block is part of a vehicle.
#                # Also serves as a means to "register" a sitting player as a passenger to a ship.
#
#   turret_base: true/false # (Boolean - Optional; Defaults to `false` if not provided)
#                # Prevents Subsegment from sticking to these blocks.
#                # Useful as a sort of "buffer" to protect a larger ship from getting torn apart by its Turret/Subsegment.
#
#   weight: Any Number # (Decimal values work - Required; may be set to `null` to be effectively ignored)
#                # Normally every block has a weight value, setting to `null` means the block will not be factored into the block count of a ship.
#                # Whereas setting the block to `0.0` will still count the towards the total blocks and weight (Although since the weight is `0.0`, it wont matter).
#                # Positive Weights make a given vehicle heavier and thus, more likely when assembled, to not fly, Negative Weights do the inverse, and adds "passive lift".
#                #
#                # (TLDR; This sets a block-type's weight/lift value.)
#
#   health: Positive Number # (Decimal values work - Optional; Default explaind below)
#                # This option defines how much health a given block has and provides to its Ship. If not specified, the amount of health is determined, in part, by a function of the Block's Weight.
#                # Once a block aboard a vehicle loses all its health (At or below 0), the ship gets dealt damage following this equation (`Ship's Current Health - (Block's Max Health + 5)`).
#                # The block then gets converted into whatever Fluid Medium it had occupied before (`Air/Water`).
#                # The initial health of each block gets added up into the Ship's overall HP Pool, however the Ship's HP only decreases when block gets broken.
#                #
#                # (TLDR; This sets a block-type's max health.)
# 
# 
# 
##
```
