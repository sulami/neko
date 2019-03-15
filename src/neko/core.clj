(ns neko.core
  (:gen-class)
  (:require [lanterna.terminal :as term]
            [clojure.string :as str]))

; Global state atom, because that's a good idea.
(def global-state (atom {:insert-mode false
                         :cursor [0 0]
                         :text ""}))

(defn redraw
  "Redraws the screen on every change to the global state."
  [key a old-state new-state]
  (let [t (:terminal new-state)]
    (term/clear t)
    (let [lines (->> new-state
                     :text
                     str/split-lines
                     (take 24))]
      (dorun (map-indexed #(term/put-string t %2 0 %1) lines)))
    (when (:insert-mode new-state)
      (term/put-string t "--INSERT--" 0 24))
    (let [[x y] (:cursor new-state)]
      (term/move-cursor t x y))))

(add-watch global-state :redraw-watcher redraw)

(defn normal-mode-input
  "Performs actions for normal-mode."
  [k]
  (case k
    \i (swap! global-state assoc-in [:insert-mode] true)
    \q (System/exit 0)
    nil))

(defn process-input
  "Processes text input so we end up with the right formats."
  [k]
  (case k
    :enter (do (swap! global-state update-in [:text] #(str % "\n"))
               (swap! global-state update-in [:cursor 1] inc)
               (swap! global-state assoc-in [:cursor 0] 0))
    :backspace (do (swap! global-state update-in [:text] #(apply str (drop-last %)))
                   (swap! global-state update-in [:cursor]
                          #(let [[x y] %]
                             (cond
                               (= x y 0) [x y]
                               (= x 0) [x (dec y)] ;; FIXME find end of previous line
                               :else [(dec x) y]))))
    (do (swap! global-state update-in [:text] #(str % k))
        (swap! global-state update-in [:cursor 0] inc))))

(defn insert-mode-input
  "Performs actions for insert-mode."
  [k]
  (case k
    :escape (swap! global-state assoc-in [:insert-mode] false)
    (process-input k)))

(defn input-loop
  "Main input loop."
  [t]
  (loop []
      (let [k (term/get-key-blocking t)]
        (prn k)
        (if (:insert-mode @global-state)
          (insert-mode-input k)
          (normal-mode-input k))
        (recur))))

(defn -main
  "Let's get this kitty started."
  [& args]
  (let [t (term/get-terminal :swing)]
    (swap! global-state assoc-in [:terminal] t)
    (term/start t)
    (input-loop t)))
