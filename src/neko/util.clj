(ns neko.util)

(defn clamp
  "Clamps `x` to the range `lower`:`upper`.

  Either limit can be `nil` if optional."
  [x lower upper]
  (cond-> x
    (some? lower) (max lower)
    (some? upper) (min upper)))
