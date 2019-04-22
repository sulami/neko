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
                   \: #(switch-mode :command)
                   \q #(System/exit 0)}}

                 :insert
                 {:default-to-write true
                  :keymap
                  {:escape #(switch-mode :normal)}}

                 :command
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
  (let [t (term/get-terminal :swing)] ;; also: text
    (swap! global-state assoc-in [:terminal] t)
    (term/start t)
    (input-loop t)))


;; exploration
(def buffer {:content (list
                       "line one"
                       "line two"
                       "line three")
             :file nil})

(def window {:buffer buffer
             :position 1
             :formatters (list
                          #(map clojure.string/capitalize %))
             :cursor [0 0]})

(defn content->markup [content]
  ;; TODO define a flexible markup format which formatters can work on
  content)

(defn apply-formatters [window]
  (let [formatters (:formatters window)]
    (update-in window
               [:buffer :content]
               (fn [x] (reduce #(%2 %1) x formatters)))))

(defn trim-window-content [window]
  (let [pos (:position window)]
    (update-in window [:buffer :content] #(drop pos %))))

;; Idea
;; Have standardised classes of tokens
;; Tag things with their "class" of colour
;; Have the frontend use colourschemes
;; Have a standardised format for schemes

(-> window
    (update-in [:buffer :content] content->markup)
    int
    apply-formatters
    trim-window-content) ; then pass to interface

;; Formatters as actions
;; we fork the buffer content for each formatter to work on (eventually off-process)
;; then merge them into one
;; ordering should be arbitrary
;; formatters return actions
;; actions are data (should probably have specs)
;; actions are limited so that conflicts are avoided. so no "rewrite this line"
;; intermediate format is still tbd
;; TODO: figure out how to prevent conflicts, eg. what happens if two insertions happen in the same place? which order will they be in?

(defn run-formatters [base formatters]
  (let [actions (map #(% base) formatters)]
    (apply concat actions)))

(defn insert-header-formatter [content]
  (list {:action :insert
         :position 0
         :content ["# header"]}))

(defn insert-footer-formatter [content]
  (list {:action :insert
         :position (count content)
         :content ["-- footer"]}))

(defn apply-action [base action]
  (case (:action action)

    :insert
    (concat (take (:position action) base)
            (:content action)
            (drop (:position action) base))))

(let [base ["abc"]]
  (->> (run-formatters base [insert-header-formatter
                             insert-footer-formatter])
       (map (partial apply-action base))))
