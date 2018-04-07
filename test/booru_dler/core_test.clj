(ns booru-dler.core-test
  (:use midje.sweet)
  (:require [booru-dler.core :refer :all]))

(facts "about `parse-args-test`"
  (fact "can join arguments over seqs"
    (parse-args) => ""
    (parse-args "hallo" "welt") => "hallo+welt"
    (parse-args ["hallo" "welt"]) => "hallo+welt"
    (parse-args ["hallo"] "welt") => "hallo+welt"
    (parse-args ["hallo" "welt"] ["test"]) => "hallo+welt+test"
    (parse-args ["hallo" "welt"]) => "hallo+welt"
    (parse-args '("hallo" "welt")) => "hallo+welt")

  (fact "translates keys into their name and supports maps"
    (parse-args :a :b :c) => "a+b+c"
    (parse-args {:a 1 :b 2}) => "a+1+b+2")

  (fact "can take ints and symbols as arguments"
    (parse-args 1 2 3) => "1+2+3"
    (parse-args 'hallo 'welt) => "hallo+welt"
    (parse-args 1 2 'hallo 'welt "test") => "1+2+hallo+welt+test"
    (parse-args [1 2 3] ['hallo]) => "1+2+3+hallo")

  (fact "supports arbitrary nesting"
    (parse-args 1 [2] [[3 4]]) => "1+2+3+4"))

(facts "about `php-query`"
  (fact "defaults to index.php if to target script is given"
    (php-query "" {}) => "/index.php?"
    (php-query "localhost:8000" {}) => "localhost:8000/index.php?")
  
  (fact "can take a specific target script"
    (php-query "" "test.php" {}) => "/test.php?"
    (php-query "localhost:8000" "test.php" {}) => "localhost:8000/test.php?")
  
  (fact "creates a URL query from a given map"
    (php-query "" {:a 1}) => "/index.php?a=1"
    (php-query "" {:a 1 :b 2}) => "/index.php?a=1&b=2"
    (php-query "" {:a ["hello" "world"] :b {:a 1}}) => "/index.php?a=hello+world&b=a+1"))

(facts "about `gelbooru-url`"
  (fact "generates a url to a gelbooru API page"
    (gelbooru-url ["tag"] 42 1) => 
    "https://gelbooru.com/index.php?page=dapi&s=post&q=index&tags=tag&limit=42&pid=0")
  
  (fact "has defaults for a empty tag seq and a ommited limit"
    (gelbooru-url [] 42 1) =>
    "https://gelbooru.com/index.php?page=dapi&s=post&q=index&tags=&limit=42&pid=0"
    (gelbooru-url ["tag"] 1) =>
    "https://gelbooru.com/index.php?page=dapi&s=post&q=index&tags=tag&limit=100&pid=0")

  (fact "calculates the page id which is dependent on the post limit"
    (gelbooru-url [] 2) =>
    "https://gelbooru.com/index.php?page=dapi&s=post&q=index&tags=&limit=100&pid=100"
    (gelbooru-url [] 4) =>
    "https://gelbooru.com/index.php?page=dapi&s=post&q=index&tags=&limit=100&pid=300"
    (gelbooru-url [] 42 2) =>
    "https://gelbooru.com/index.php?page=dapi&s=post&q=index&tags=&limit=42&pid=42"
    (gelbooru-url [] 42 3) =>
    "https://gelbooru.com/index.php?page=dapi&s=post&q=index&tags=&limit=42&pid=84")

  (fact "can join multiple given tags"
    (gelbooru-url ["hallo" "welt"] 1) =>
    "https://gelbooru.com/index.php?page=dapi&s=post&q=index&tags=hallo+welt&limit=100&pid=0"
    (gelbooru-url ["hallo" ["welt"]] 1) =>
    "https://gelbooru.com/index.php?page=dapi&s=post&q=index&tags=hallo+welt&limit=100&pid=0"))

(facts "about `gelbooru-urls`"
  (fact "equivalent to making a list of multiple gelbooru urls"
    (gelbooru-urls [] 42 [1 2 3]) => 
    (list (gelbooru-url [] 42 1) (gelbooru-url [] 42 2) (gelbooru-url [] 42 3))
    (gelbooru-urls ["test" "1"] 42 [1 2]) =>
    (list (gelbooru-url ["test" "1"] 42 1) (gelbooru-url ["test" "1"] 42 2)))

  (fact "the limit parameter defaults to 100"
    (gelbooru-urls [] [1 2 3]) =>
    (list (gelbooru-url [] 100 1) (gelbooru-url [] 100 2) (gelbooru-url [] 100 3))))

(facts "about `dev-null`"
  (fact "returns `nil` no matter which parameters it is given"
    (dev-null 1 2 3) => nil
    (dev-null 'hallo "welt") => nil
    (dev-null #'+ #'print 1 2 3 "test" [1 2 "some args"]) => nil))

(facts "about `range?`"
  (fact "Returns a list of start and end of a given range string for valid ranges"
    (range? "1-1") => '(1 1)
    (range? "1-3") => '(1 3)
    )
  
  (fact "Returns nil for any invalid range (invalid numbers or argument count"
    (range? "1") => nil
    (range? "1-2-3") => nil
    (range? "hallo") => nil
    (range? "ha-llo") => nil
    ))

(facts "about `parse-range`"
  (fact "parses a range string such as \"1-3\" into a range of ints"
    (parse-range "1-1") => '(1)
    (parse-range "1-3") => '(1 2 3)
    (parse-range "1-0") => '())
  
  (fact "any invalid range is parsed to `nil`"
    (parse-range "1-") => nil
    (parse-range "hal-lo") => nil
    (parse-range "1") => nil))

(facts "about `parse-pages`"
  (fact "parses integers into a single item list"
    (parse-pages "1") => [1])

  (fact "parses ranges into a list of all ints in range"
    (parse-pages "1-3") => [1 2 3]
    (parse-pages "1-1") => [1]
    (let [n (inc (rand-int 20))
          m (+ n (rand-int 100))
          r (range n (inc m))]
      (parse-pages (str n "-" m)) => r))

  (fact "allows arbitrary combinations of both where atoms of the expression are delimited by `,`"
    (parse-pages "1,2,3") => [1 2 3]
    (parse-pages "1-3,5-7") => [1 2 3 5 6 7]
    (parse-pages "1,3,7-9") => [1,3,7,8,9]))
