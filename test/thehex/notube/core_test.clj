(ns thehex.notube.core-test
  (:require  [clojure.test :refer :all]
             [thehex.notube.core :refer :all]))

(deftest to-do-test
  (testing "returns 9"
    (is (= 9 (to-do 3)))))

