(ns neko.core
  (:gen-class)
  (:require [lanterna.terminal :as term]
            [clojure.string :as str]))

; Global state atom, because that's a good idea.
(def global-state (atom {:active-mode :normal
                         :cursor [0 0]
                         :text ""}))

(defn switch-mode
  [mode]
  (swap! global-state assoc-in [:active-mode] mode))

(def config
  (atom {:modes {:normal
                 {:default-to-write false
                  :keymap
                  {\i #(switch-mode :insert)
                   \q #(System/exit 0)}}

                 :insert
                 {:default-to-write true
                  :keymap
                  {:escape #(switch-mode :normal)}}}}))

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
    (term/put-string t (str (:active-mode new-state)) 0 24)
    (let [[x y] (:cursor new-state)]
      (term/move-cursor t x y))))

(add-watch global-state :redraw-watcher redraw)

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

(defn handle-input
  [mode k]
  (let [keymap (:keymap mode)]
    (cond
      (contains? keymap k) ((get keymap k))
      (:default-to-write mode) (process-input k)
      :else nil)))

(defn input-loop
  "Main input loop."
  [t]
  (loop []
    (let [k (term/get-key-blocking t)
          mode ((:active-mode @global-state) (:modes @config))]
      (handle-input mode k)
      (recur))))

(defn -main
  "Let's get this kitty started."
  [& args]
  (let [t (term/get-terminal :swing)]
    (swap! global-state assoc-in [:terminal] t)
    (term/start t)
    (input-loop t)))
