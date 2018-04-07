(ns booru-dler.core
  (:require [clojure.string :as str]
            [clojure.xml :as xml]
            [clojure.tools.cli :refer [parse-opts]])
  (:gen-class))

(defn parse-args
  "Parses the arguments supplied to a query parameter"
  [& args]
  (str/join "+" (map (fn [arg] (cond (string? arg) arg
                                     (sequential? arg) (apply parse-args arg)
                                     (coll? arg) (apply parse-args (seq arg))
                                     (keyword? arg) (name arg)
                                     :else (str arg)))
                     args)))

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

(defn gelbooru-url
  "Creates a single URL to gelbooru from a list of tags, a limit and a given page"
  ([tags limit page]
   (php-query "https://gelbooru.com"
              {:page "dapi"
               :s "post"
               :q "index"
               :tags (if (empty? tags) "" tags)
               :limit limit
               :pid (* (dec page) limit)}))
  ([tags page]
   (gelbooru-url tags 100 page)))

(defn gelbooru-urls
  "Create a list of URLs to gelbooru from a list of given tags, page sizes and pages"
  ([tags limit pages]
   (map (partial gelbooru-url tags limit) pages))
  ([tags pages] 
   (gelbooru-urls tags 100 pages)))

(defn pparse
  "Applies clojure.xml/parse in parallel to a list of URLs"
  [urls]
  (pmap xml/parse urls))

(defn extract-image-url
  "Retrieves all URLs to full size images from a pre-parsed gelbooru XML document"
  [xml]
  (map (fn [post] (:file_url (:attrs post))) (:content xml)))

(defn query-to-urls
  "Parses a query to gelbooru into a list of images"
  ([tags limit pages]
   (-> (gelbooru-urls tags limit pages)
       (pparse)
       (first)
       (extract-image-url)
       (flatten)))
  ([tags pages] 
   (query-to-urls tags 100 pages)))

(defn non-concurrent
  [tags pages]
  (-> (gelbooru-urls tags 100 pages)
      ((partial map xml/parse))
      (first)
      (extract-image-url)
      (flatten)))

(defn download-image-to
  [directory url name]
  (spit (clojure.java.io/file directory name) (slurp url) :append false))

(defn download-to
  [directory urls]
  (map (fn [url]
         (print "test")
         (let [f-name (last (str/split url #"/"))]
           (print "Downloading from" url "to" (str directory "/" f-name))
           (download-image-to directory url f-name)))
       urls))

(defn dev-null
  "Discards any args, can be used to suppress return values"
  [& args]
  nil)

(defn download-images
  ([directory tags limit pages]
   (print "Downloading pages" pages "at" limit "images per page with tags" tags "to" directory)
   (let [imgs (query-to-urls tags limit pages)]
     (download-to directory imgs)))
  ([directory tags pages]
   (download-images directory tags 100 pages)))

(defn range?
  [r]
  (let [s (str/split r #"-")]
    (when (= 2 (count s)) (try (doall (map #(Integer/parseInt %) s))
                             (catch Exception e)))))

(defn parse-range
  [r]
  (let [r? (range? r)] 
    (when r? (range (first r?) (inc (second r?))))))

(defn parse-pages 
  [pages]
  (flatten (map (fn [i] (if (range? i) (parse-range i) (Integer/parseInt i))) 
                (str/split pages #","))))

(def cli-options
  [["-t" "--tags TAGS" "Tags for the search query"
    :default []
    :parse-fn #(str/split % #" ")]
   ["-d" "--directory DOWNLOAD-DIRECTORY" "Directory to download to"
    :default (System/getProperty "user.dir")]
   ["-p" "--pages PAGES" "Pages to download (form: \"1,2,3,4-6\""
    :default nil
    :parse-fn parse-pages]
   ["-l" "--limit LIMIT" "Image limit per page"
    :default 100
    :parse-fn #(Integer/parseInt %)]
   ["-h" "--help"]])

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (let [opts (parse-opts args cli-options)]
    (if (get-in opts [:options :help])
      (print (:summary opts))
      (if (not-empty (:errors opts))
        (print (:errors opts))
        (let [tags (get-in opts [:options :tags])
              limit (get-in opts [:options :limit])
              pages (get-in opts [:options :pages])
              dir (get-in opts [:options :directory])]
          (download-images dir tags limit pages))))))
