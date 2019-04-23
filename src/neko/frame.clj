(ns neko.frame)

(defprotocol Frame
  (open [this] "Opens the new frame")
  (dimensions [this] "Returns the current dimensions of the frame.")
  (erase [this] "Clears the screen.")
  (draw [this position content] "Draws `content` at `position`.")
  (move-cursor [this position] "Moves the (system-)cursor to `position`."))

;; TODO needs temp wingows somehow, like popups
;; one at a time (per window) is fine, and they don't really need to be full-blown windows/buffers

;; TODO needs a way to send things to the status line

;; TODO needs a way to callback for events
;; - window size changed
;; - key input
;; - frame closed
;; - other stuff?
;; one for these should probably be fine, and then just spec the event types
