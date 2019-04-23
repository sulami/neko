(ns neko.spec
  (:require [clojure.spec.alpha :as s]))

(s/def ::file (s/nilable string?))

(s/def ::buffer
  (s/keys :req-un [::content
                   ::file]))

(s/fdef ::formatter
  :args ::buffer
  :ret ::buffer)

(s/def ::position pos-int?)
(s/def ::formatters
  (s/coll-of ::formatter))
(s/def ::cursor
  (s/tuple pos-int? pos-int?))

(s/def ::window
  (s/keys :req-un [::buffer
                   ::position
                   ::formatters
                   ::cursor]))
