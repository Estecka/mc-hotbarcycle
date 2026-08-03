package com.github.nyuppo;

import static com.github.nyuppo.HotbarCycleClient.isRowEnabled;
import static com.github.nyuppo.HotbarCycleClient.isColumnEnabled;

/**
 * Utility for  simulating the  cycling of rows. Used  to precompute  cycling by
 * multiple steps at once, and reduce the amount of packets sent to the server.
 */
public class SwapMap
{
	/**
	 * Simulates the cycling of one row by 1 space up.
	 * @param row The index of the row containing items to move.
	 * @return If the row is enabled, returns  the next enabled row, i.e the row
	 * where the items  must be moved. If the row  is disabled, returns the same
	 * index.
	 */
	static public int CycleRow(int row){
		if (!isRowEnabled(row))
			return row;

		for (int offset=1; offset<4; ++offset) {
			int dst = (row + offset) % 4;
			if (isRowEnabled(dst))
				return dst;
		}

		return row;
	}

	/**
	 * Simulates the cycling of all rows by 1 space up.
	 * @return For each row, points  to the row  where its items  must be moved.
	 * Disabled rows point to themselves.
	 */
	static public int[]	CycleAllRows(){
		int [] rolloverMap = new int[4];

		for (int src=0; src<4; ++src)
			rolloverMap[src] = CycleRow(src);

		return rolloverMap;
	}


	/**
	 * Simulates the cycling of all rows by an arbitrary amount.
	 * @param direction The amount  of  cycles.  Positive  values  cycle upward,
	 * negative values cycle downward.
	 * @return For each row, points  to the row  where its items  must be moved.
	 * Disabled rows point to themselves.
	 */
	static public int[]	CycleAllRows(int direction){
		int[] rolloverMap = CycleAllRows();
		int[] swapMap = new int[4];

		int swapableRowCount = 0;
		for (int i=0; i<4; ++i)
			if (isRowEnabled(i))
				swapableRowCount++;
		direction %= swapableRowCount;
		if (direction < 0)
			direction += swapableRowCount;

		for (int i=0; i<4; ++i){
			swapMap[i] = i;
			for (int n=0; n<direction; ++n)
				swapMap[i] = rolloverMap[swapMap[i]];
		}

		return swapMap;
	}

	/**
	 * Simulates the cycling of every inventory slot by an arbitrary amount.
	 * @param direction The amount  of  cycles.  Positive  values  cycle upward,
	 * negative values cycle downward.
	 * @return For each slot index, points to the resulting slot. Disabled slots
	 * point to themselves.
	 * @deprecated Overkill. CycleAllRows, is enough for most situations.
	 */
	@Deprecated
	static public int[]	CycleInventory(int direction){
		int[] swapMap = new int[4*9];
		int[] rowSwap = CycleAllRows(direction);

		for (int i=0; i<swapMap.length; ++i){
			int x = i % 9;
			int y = i / 9;

			if (!isColumnEnabled(x) || !isRowEnabled(y))
				swapMap[i] = i;
			else 
				swapMap[i] = (rowSwap[y] * 9) + x;
		}

		return swapMap;
	}
}
