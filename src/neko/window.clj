(ns neko.window
  (:require [neko.spec :as spec]
            [neko.util :as util]))

#_(def window {:buffer buffer
             :position 1
             :formatters (list
                          #(map clojure.string/capitalize %))
             :cursor [0 0]})

(defn move-cursor
  "Moves a cursor of `window` to [`x` `y`].

  If the target coordinates are out of bounds, moves as close as possible.
  Returns the updated `window`."
  [window [x y]]
  (let [x (util/clamp x 0 nil)
        y (util/clamp y 0 nil)]
    (assoc window :cursor [x y])))
