(ns gitgene.levenshtein-test
  (:use midje.sweet
        gitgene.levenshtein)
  (:require [clojure.string]))

(fact
 "distance calculates the levenshtein distance between two strings"
 (distance "hi" "hi") => 0
 (distance "kitten" "sitting") => 3
 (distance "Saturday" "Sunday") => 3
 (distance "Sunday" "Saturday") => 3
 (distance "Graeme" "Greme") => 1)
