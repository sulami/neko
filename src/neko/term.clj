(ns neko.term
  (:require [neko.frame :as frame]
            [lanterna.terminal :as term]))

(defrecord Terminal []
  frame/Frame

  (open [this]
    (let [t (term/get-terminal :swing)]
      (term/start t)
      (assoc this :terminal t)))

  (dimensions [this]
    (term/get-size (:terminal this)))

  (erase [this]
    (term/clear (:terminal this)))

  (draw [this position content]
    (let [t (:terminal this)
          [x y] position]
      (term/put-string t content x y)))

  (move-cursor [this position]
    (let [[x y] position]
      (term/move-cursor (:terminal this) x y))))
