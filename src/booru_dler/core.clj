(ns booru-dler.core
  (:require [clojure.string :as str])
  (:gen-class))

(defn parse-args
  "Parses the arguments supplied to a query parameter"
  [args]
  (cond (string? args) args
        (sequential? args) (str/join "+" args)
        (coll? args) (parse-args (seq args))
        :else (str args)))

(defn php-query 
  "Creates a PHP-style query URL"
  ([page script query-vect]
   (let [queries (str/join "&" 
                           (map (fn [[param val]] 
                                  (str (name param) "=" (parse-args val)))
                                query-vect))]
     (str page "/" script "?" queries)))
  ([page query-vect] 
   (php-query page "index.php" query-vect)))

(defn gelbooru-urls
  "Create a list of URLs to gelbooru from a list of given tags, page sizes and pages"
  ([tags limit pages]
   (map (fn [page]
          (php-query "https://gelbooru.com"
                     {:page "dapi"
                      :s "post"
                      :q "index"
                      :tags tags
                      :limit limit
                      :pid (* (dec page) limit)}))
        pages))
  ([tags pages] 
   (gelbooru-urls tags 100 pages)))

(defn download-from
  "Download from a list of urls"
  [urls]
  (pmap slurp urls))

(defn next-element
  "Breakes the next element (either text or a opening or closing tag) from a xml document"
  [xml-string]
  (if (= "<" (subs xml-string 0 1))
    (let [[tag rest] (str/split xml-string #">" 2)]
      [(str tag ">") rest])
    (let [[tag rest] (str/split xml-string #"<" 2)]
      [tag (str "<" rest)])))

(defn element-split
  "Split a XML doc into elements (opening/closing tags and body strings)"
  [xml-string]
  (loop [res []
         rem xml-string]
    (if (empty? rem)
      res
      (let [[element rest] (next-element rem)]
        (recur (conj res element) rest)))))

(defn unwrap
  "Removes the first and last character of a string"
  [string]
  (subs string 1 (dec (count string))))

(defn opening-tag?
  "Check if a tag is a opening tag"
  [tag]
  (= "<" (subs tag 0 1)))

(defn empty-tag?
  "Determines whether a tag is a empty tag"
  [tag]
  (let [len (count tag)]
    (and (> 2 len)
         (opening-tag? tag) 
         (= "/>" (subs tag (- len 2))))))

(defn closing-tag?
  "Check if a tag is a closing tag"
  [tag]
  (= "</" (subs tag 2)))

(defn tag-info
  "Parses a tag and creates a vector of the form [tag-name {:arg val ...}]"
  [tag]
  (let [[name & attributes] (str/split (unwrap tag) #" ")]
    (loop [rem attributes
           res {}]
      (if (empty? rem)
        [name res]
        (let [[curr & next] rem
              [attr-name attr-val] (str/split curr #"=")]
          (recur next (assoc res (keyword attr-name) (unwrap attr-val))))))))

(defn get-tag-name
  "Returns the name of a given XML tag as a key argument"
  [tag]
  (take-while #(not= % " ") (subs tag 1)))

(defn parse-xml
  [doc]
  (let [tags (element-split doc)]
    tags))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
