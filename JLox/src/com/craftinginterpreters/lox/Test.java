package com.craftinginterpreters.lox;

public class Test {
	public static void main(String args[]) {
		
//		int[] listNumbers = { 1, 3, 10, 19, 25, 33, 100, 190, 203, 5000 };
//		
//		if (binSearch(201, listNumbers)) {
//			System.out.println("Yes");
//		} else {
//			System.out.println("No, it's fail!!");
//		}
		
		Test ob = new Test();
        int arr[] = { 1, 3, 10, 19, 25, 33, 100, 190, 203, 5000 };
        int x = 5000;
        int result = ob.binarySearch(arr, x);
        if (result == -1)
            System.out.println("Element not present");
        else
            System.out.println("Element found at "
                               + "index " + result);
	}
	
	int binarySearch(int arr[], int x)
    {
        int l = 0, r = arr.length - 1;
        while (l <= r) {
            int m = l + (r - l) / 2;
 
            // Check if x is present at mid
            if (arr[m] == x)
                return m;
 
            // If x greater, ignore left half
            if (arr[m] < x)
                l = m + 1;
 
            // If x is smaller, ignore right half
            else
                r = m - 1;
        }
 
        // if we reach here, then element was
        // not present
        return -1;
    }
	
	private static boolean binSearch(final int n, final int[] listNumbers) {
		int len = listNumbers.length - 1;
		return binSearchHelper(0, len, n, listNumbers);
	}
	
	private static boolean binSearchHelper(final int head, final int tail, final int n, final int[] listNumbers) {
		int average = (head + tail) / 2;
		
		if (average < 1 || (tail - head) == 1) {
			return false;
		}
		
		if (n == listNumbers[average] || n == listNumbers[head] || n == listNumbers[tail]) {
			return true;
		} else if (n > listNumbers[average]){
			return binSearchHelper(average, tail, n, listNumbers);
		} else if (n < listNumbers[average]) {
			return binSearchHelper(head, average, n, listNumbers);
		}
		return false;
	}
}
