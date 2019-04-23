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

;; Formatters as actions
;; we fork the buffer content for each formatter to work on (eventually off-process)
;; then merge them into one
;; ordering should be arbitrary
;; formatters return actions
;; actions are data (should probably have specs)
;; actions are limited so that conflicts are avoided. so no "rewrite this line"
;; intermediate format is still tbd
;; TODO: figure out how to prevent conflicts, eg. what happens if two insertions happen in the same place? which order will they be in?
;; maybe just have various IDs on actions, so we can sort them later?
;; possible actions: insert

(defn run-formatters [base formatters]
  (let [actions (map #(% base) formatters)]
    (apply concat actions)))

(defn insert-header-formatter [content]
  (list {:origin :insert-header
         :sequence-number 1
         :action :insert
         :position 0
         :content ["# header"]}))

(defn insert-footer-formatter [content]
  (list {:origin :insert-footer
         :sequence-number 1
         :action :insert
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
       (sort-by (juxt :origin :sequence-number))
       #_(map (partial apply-action base))))


;; Commands
;; Everything you can do is going to be a command of sorts
;; Commands are just functions, really
;; They might have a special form, like in emacs, so we can pass in special state and stuff
;; Commands should be able to trigger a lightweight state, so that command chains are possible, vim-style
;; They should also be inspectable in a tree-form, for documentation
;; this might be implemented in the form of keymaps, just like in emacs
;; there should be a standard way of prompting for options, to eliminate duplicate functions
;; so a function for changing a buffer in a window could take either a buffer directly, or instructions for how to prompt for one
