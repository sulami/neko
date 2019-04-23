(ns neko.window
  (:require [neko.util :as util]))

#_(def window {:buffer buffer
             :position 1
             :formatters (list
                          #(map clojure.string/capitalize %))
             :cursor [0 0]})

(defn move-cursor
  "Moves `window`'s cursor to [`x` `y`].

  If the target coordinates are out of bounds, moves as close as possible.
  Returns the updated `window`."
  [window [x y]]
  (let [x (util/clamp x 0 nil)
        y (util/clamp y 0 nil)]
    (assoc window :cursor [x y])))

(defn move-cursor-relative
  "Moves `window`'s cursor relatively to its current position.

  Behaves the same way as `neko.window.move-cursor`."
  [window [x y]]
  (->> [x y]
       (map + (:cursor window))
       (move-cursor window)))
