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
