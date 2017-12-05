(ns booru-dler.core
  (:gen-class))

(defn parse-args
  "Parses the arguments supplied to a query parameter"
  [args]
  (cond (string? args) args
        (sequential? args) (clojure.string/join "+" args)
        (coll? args) (parse-args (seq args))
        :else (str args)))

(defn php-query 
  "Creates a PHP-style query URL"
  ([page script query-vect]
   (let [queries (clojure.string/join "&" 
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

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
