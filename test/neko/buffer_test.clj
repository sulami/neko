(ns neko.buffer-test
  (:require [neko.buffer :as sut]
            [clojure.test :refer :all]))

(def buffer-test-stub {})

(deftest content-dimension-test
  (testing "it reports the length of the buffer content"
    (let [content [1 2 3]]
      (is (= (count content)
             (-> buffer-test-stub
                 (assoc :content content)
                 sut/content-length))))))
