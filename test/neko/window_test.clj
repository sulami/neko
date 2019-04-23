(ns neko.window-test
  (:require [neko.window :as sut]
            [clojure.test :refer :all]))

(def test-window-stub {})

(deftest move-cursor-test
  (testing "moving the cursor absolutely"
    (testing "moves it to the target coordinates"
      (let [target [1 1]]
        (is (= target
               (-> test-window-stub
                   (sut/move-cursor target)
                   :cursor)))))

    (testing "to negative coordinates moves it to 0,0"
      (is (= [0 0]
             (-> test-window-stub
                 (sut/move-cursor [-1 -1])
                 :cursor)))))

  ;; TODO test out of bounds on the right & bottom
  ;; for that I need methods to find the buffer content dimensions

  (testing "moving the cursor relatively"
    (testing "moves it to the target coordinates"
      (is (= [2 2]
             (-> test-window-stub
                 (assoc :cursor [1 1])
                 (sut/move-cursor-relative [1 1])
                 :cursor))))

    (testing "to negative coordinates moves it to 0,0"
      (is (= [0 0]
             (-> test-window-stub
                 (assoc :cursor [1 1])
                 (sut/move-cursor-relative [-2 -2])
                 :cursor))))))
