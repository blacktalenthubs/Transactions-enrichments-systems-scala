
---

```scala
// 1. Two Sum (HashMap)
def twoSum(nums: Array[Int], target: Int): Array[Int] = {
  val map = scala.collection.mutable.Map[Int, Int]()
  for (i <- nums.indices) {
    val complement = target - nums(i)
    if (map.contains(complement)) return Array(map(complement), i)
    map(nums(i)) = i
  }
  Array(-1, -1) // Should never reach here if exactly one solution
}
```

```scala
// 2. Contains Duplicate (Set)
def containsDuplicate(nums: Array[Int]): Boolean = {
  val seen = scala.collection.mutable.Set[Int]()
  for (n <- nums) {
    if (seen.contains(n)) return true
    seen += n
  }
  false
}
```

```scala
// 3. Valid Anagram (HashMap)
def isAnagram(s: String, t: String): Boolean = {
  if (s.length != t.length) return false
  val count = scala.collection.mutable.Map[Char, Int]().withDefaultValue(0)
  for (ch <- s) count(ch) += 1
  for (ch <- t) {
    if (!count.contains(ch) || count(ch) == 0) return false
    count(ch) -= 1
  }
  true
}
```

```scala
// 4. First Unique Character in a String (HashMap)
def firstUniqChar(s: String): Int = {
  val freq = scala.collection.mutable.Map[Char, Int]().withDefaultValue(0)
  for (ch <- s) freq(ch) += 1
  for (i <- s.indices) {
    if (freq(s(i)) == 1) return i
  }
  -1
}
```

```scala
// 5. Intersection of Two Arrays (Set)
def intersection(nums1: Array[Int], nums2: Array[Int]): Array[Int] = {
  val set1 = nums1.toSet
  val set2 = nums2.toSet
  (set1 intersect set2).toArray
}
```

```scala
// 6. Intersection of Two Arrays II (HashMap)
def intersect(nums1: Array[Int], nums2: Array[Int]): Array[Int] = {
  val freq = scala.collection.mutable.Map[Int, Int]().withDefaultValue(0)
  for (n <- nums1) freq(n) += 1
  val res = scala.collection.mutable.Buffer[Int]()
  for (n <- nums2) {
    if (freq(n) > 0) {
      res += n
      freq(n) -= 1
    }
  }
  res.toArray
}
```

```scala
// 7. Remove Duplicates from Sorted Array (Two Pointers)
def removeDuplicates(nums: Array[Int]): Int = {
  if (nums.isEmpty) return 0
  var idx = 0
  for (i <- 1 until nums.length) {
    if (nums(i) != nums(idx)) {
      idx += 1
      nums(idx) = nums(i)
    }
  }
  idx + 1
}
```

```scala
// 8. Valid Palindrome (Two Pointers)
def isPalindrome(s: String): Boolean = {
  val filtered = s.toLowerCase.filter(_.isLetterOrDigit)
  var left = 0
  var right = filtered.length - 1
  while (left < right) {
    if (filtered(left) != filtered(right)) return false
    left += 1
    right -= 1
  }
  true
}
```

```scala
// 9. Reverse String (Two Pointers)
def reverseString(s: Array[Char]): Unit = {
  var left = 0
  var right = s.length - 1
  while (left < right) {
    val tmp = s(left)
    s(left) = s(right)
    s(right) = tmp
    left += 1
    right -= 1
  }
}
```

```scala
// 10. Reverse Vowels of a String (Two Pointers)
def reverseVowels(s: String): String = {
  val vowels = Set('a','e','i','o','u','A','E','I','O','U')
  val arr = s.toCharArray
  var left = 0
  var right = arr.length - 1
  while (left < right) {
    while (left < right && !vowels.contains(arr(left))) left += 1
    while (left < right && !vowels.contains(arr(right))) right -= 1
    val tmp = arr(left)
    arr(left) = arr(right)
    arr(right) = tmp
    left += 1
    right -= 1
  }
  new String(arr)
}
```

```scala
// 11. Move Zeroes (Two Pointers)
def moveZeroes(nums: Array[Int]): Unit = {
  var pos = 0
  for (i <- nums.indices) {
    if (nums(i) != 0) {
      val tmp = nums(pos)
      nums(pos) = nums(i)
      nums(i) = tmp
      pos += 1
    }
  }
}
```

```scala
// 12. Remove Element (Two Pointers)
def removeElement(nums: Array[Int], value: Int): Int = {
  var index = 0
  for (i <- nums.indices) {
    if (nums(i) != value) {
      nums(index) = nums(i)
      index += 1
    }
  }
  index
}
```

```scala
// 13. Sort Array By Parity (Two Pointers)
def sortArrayByParity(nums: Array[Int]): Array[Int] = {
  var left = 0
  var right = nums.length - 1
  while (left < right) {
    if (nums(left) % 2 > nums(right) % 2) {
      val tmp = nums(left)
      nums(left) = nums(right)
      nums(right) = tmp
    }
    if (nums(left) % 2 == 0) left += 1
    if (nums(right) % 2 == 1) right -= 1
  }
  nums
}
```

```scala
// 14. Two Sum II - Input Array Is Sorted (Two Pointers)
def twoSumII(numbers: Array[Int], target: Int): Array[Int] = {
  var left = 0
  var right = numbers.length - 1
  while (left < right) {
    val sum = numbers(left) + numbers(right)
    if (sum == target) return Array(left + 1, right + 1)
    else if (sum < target) left += 1
    else right -= 1
  }
  Array(-1, -1)
}
```

```scala
// 15. Isomorphic Strings (HashMap)
def isIsomorphic(s: String, t: String): Boolean = {
  if (s.length != t.length) return false
  val mapST = scala.collection.mutable.Map[Char, Char]()
  val mapTS = scala.collection.mutable.Map[Char, Char]()
  for (i <- s.indices) {
    val c1 = s(i)
    val c2 = t(i)
    if ((mapST.contains(c1) && mapST(c1) != c2) ||
        (mapTS.contains(c2) && mapTS(c2) != c1)) {
      return false
    }
    mapST(c1) = c2
    mapTS(c2) = c1
  }
  true
}
```

```scala
// 16. Contains Duplicate II (HashMap)
def containsNearbyDuplicate(nums: Array[Int], k: Int): Boolean = {
  val lastIndex = scala.collection.mutable.Map[Int, Int]()
  for (i <- nums.indices) {
    if (lastIndex.contains(nums(i)) && i - lastIndex(nums(i)) <= k) return true
    lastIndex(nums(i)) = i
  }
  false
}
```

```scala
// 17. Single Number (XOR for brevity)
def singleNumber(nums: Array[Int]): Int = {
  var xor = 0
  for (n <- nums) xor ^= n
  xor
}
```

```scala
// 18. Ransom Note (HashMap)
def canConstruct(ransomNote: String, magazine: String): Boolean = {
  val freq = scala.collection.mutable.Map[Char, Int]().withDefaultValue(0)
  for (ch <- magazine) freq(ch) += 1
  for (ch <- ransomNote) {
    if (freq(ch) <= 0) return false
    freq(ch) -= 1
  }
  true
}
```

```scala
// 19. Missing Number (Sum Formula)
def missingNumber(nums: Array[Int]): Int = {
  val n = nums.length
  val expectedSum = n * (n + 1) / 2
  val actualSum = nums.sum
  expectedSum - actualSum
}
```

```scala
// 20. Longest Common Prefix (String checks)
def longestCommonPrefix(strs: Array[String]): String = {
  if (strs.isEmpty) return ""
  var prefix = strs(0)
  for (i <- 1 until strs.length) {
    while (strs(i).indexOf(prefix) != 0) {
      prefix = prefix.substring(0, prefix.length - 1)
      if (prefix.isEmpty) return ""
    }
  }
  prefix
}
```

```scala
// 21. Implement strStr() (Naive substring)
def strStr(haystack: String, needle: String): Int = {
  if (needle.isEmpty) return 0
  for (i <- 0 to haystack.length - needle.length) {
    if (haystack.substring(i, i + needle.length) == needle) return i
  }
  -1
}
```

```scala
// 22. Excel Sheet Column Title
def convertToTitle(columnNumber: Int): String = {
  var n = columnNumber
  val sb = new StringBuilder
  while (n > 0) {
    val rem = (n - 1) % 26
    sb.insert(0, ('A' + rem).toChar)
    n = (n - 1) / 26
  }
  sb.toString
}
```

```scala
// 23. Reverse Words in a String III
def reverseWords(s: String): String = {
  s.split(" ").map(_.reverse).mkString(" ")
}
```

```scala
// 24. Palindrome Linked List (Two Pointers + Reverse)
case class ListNode(var x: Int, var next: ListNode = null)

def isPalindrome(head: ListNode): Boolean = {
  if (head == null || head.next == null) return true
  var slow, fast = head
  while (fast != null && fast.next != null) {
    slow = slow.next
    fast = fast.next.next
  }
  // reverse second half
  var prev: ListNode = null
  var curr = slow
  while (curr != null) {
    val temp = curr.next
    curr.next = prev
    prev = curr
    curr = temp
  }
  // compare halves
  var left = head
  var right = prev
  while (right != null) {
    if (left.x != right.x) return false
    left = left.next
    right = right.next
  }
  true
}
```

```scala
// 25. Valid Parentheses (Stack + HashMap)
def isValid(s: String): Boolean = {
  val map = Map(')' -> '(', ']' -> '[', '}' -> '{')
  val stack = scala.collection.mutable.Stack[Char]()
  for (ch <- s) {
    if (!map.contains(ch)) stack.push(ch)
    else {
      if (stack.isEmpty || stack.pop() != map(ch)) return false
    }
  }
  stack.isEmpty
}
```
