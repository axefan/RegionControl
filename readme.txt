REGION CONTROL PLUGIN

Advanced region control for Bukkit.

Passing player inventory from region A to region B:

								|-------------------|           |-------------------|
								|                   |           |                   |
								|     REGION A      |           |     REGION B      |
								|                   |-----------|                   |
		PLAYER >----1-------------------2-----------3-------------------4--->
								|                   |-----------|                   |
								|                   |           |                   |
								|                   |           |                   |
								|-------------------|           |-------------------|

1. When player enters (or lock) A, inventory is set.  /rc update A setInventory 1 (or 2)
2. When player leaves (or unlock) A, inventory is not restored.  /rc update A restoreInventory 0
3. When player enters (or lock) B, inventory is not set. /rc update B setInventory 0
4. When player leaves (or unlock) B, inventory is restored. /rc update B restoreInventory 1 (or 2)

Warning, if the player dies while between regions, the player's inventory needs to be restored.
We need to monitor for deaths and check saved items as well as locks even when not in a region!