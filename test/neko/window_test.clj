(ns neko.window-test
  (:require [neko.window :as sut]
            [clojure.test :refer :all]))

(deftest move-cursor-test
  (testing "moving the cursor absolutely"
    (testing "moves it to the target coordinates"
      (let [target [1 1]]
        (is (= target
               (-> {:cursor [0 0]}
                   (sut/move-cursor target)
                   :cursor)))))

    (testing "to negative coordinates moves it to 0,0"
      (is (= [0 0]
             (-> {:cursor [1 1]}
                 (sut/move-cursor [-1 -1])
                 :cursor)))))

  ;; TODO test out of bounds on the right & bottom
  ;; for that I need methods to find the buffer content dimensions
  )
