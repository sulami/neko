(ns neko.core
  (:gen-class)
  (:require [lanterna.terminal :as term]
            [clojure.string :as str]))

; Global state atom, because that's a good idea.
(def global-state (atom {:insert-mode false
                         :text ""}))

(defn redraw
  "Redraws the screen on every change to the global state."
  [key a old-state new-state]
  (let [t (:terminal new-state)]
    (term/clear t)
    (let [lines (str/split-lines (:text new-state))]
      (dorun (map-indexed #(term/put-string t %2 0 %1) lines)))
    (when (:insert-mode new-state)
      (term/put-string t "--INSERT--" 0 24))))

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
    :enter (swap! global-state update-in [:text] #(str % "\n"))
    :backspace (swap! global-state update-in [:text] #(apply str (drop-last %)))
    (swap! global-state update-in [:text] #(str % k))))

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
