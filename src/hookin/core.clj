(ns hookin.core
  (:require [bultitude.core]
            [hookin.classloader]))

(def
  #^{:dynamic true
     :doc
     "A keyword specifying which meta data key to check for hook information.
  Default value: :hook"}
  *hook-keyword* :hook)

(defonce
  #^{:doc
     "A map containing all the loaded transformation functions
  associated with the specified hooks"}
  hooked-transformations (atom {}))

(defmacro hookfn [name hook-name args body]
  "Defines a function with the given name, arguments and body and
  adds a HookIn meta data entry to be used by HookIn autoload"
  `(defn
     ~(with-meta name {*hook-keyword* hook})
     ~args ~body))

(defn- register-hooked-transformation
  "Registers a transformation function to a given key in the transformations map"
  [hook-name function]
  (do (swap! hooked-transformations assoc hook-name
             (distinct (conj (hook-name @hooked-transformations) function)))
      nil))

(defmacro register-transformation
  "Wrapper to the register-hooked-transformation function, to ensure var conversion"
  [hook-name function]
  `(register-hooked-transformation ~hook-name (var ~function)))

(defmacro autoregister-transformation
  "Tries to auto register a transformation function to the hook manager, based
  on its metadata"
  [function]
  `(register-hooked-transformation (*hook-keyword* (meta (var ~function))) (var ~function)))

(defn- autoregister-transformations
  "Adds a collection of transformation functions to the hook manager"
  [functions]
  (doseq [function functions]
    (register-hooked-transformation (*hook-keyword* (meta function)) function)))

(defn- list-hook-functions
  "Gives a list of all functions, containing a meta
  data entry with the given hook keyword, in a given namespace"
  [ns]
  (filter #(*hook-keyword* (meta %)) (vals (ns-publics ns))))

(defn load-hooks-file
  "Loads the specific clj file, and adds all hook functions in the
  files namespace to the hook manager"
  [file]
  (load-file file)
  (autoregister-transformations
   (list-hook-functions
    (first (#'bultitude.core/namespaces-in-dir file)))))

(defn load-hooks-jar
  "Adds the specific jar file to the classpath and requires the namespaces
  in the jar file and adds the hook functions to the hook manager"
  [jar-file]
  (hookin.classloader/add-classpath jar-file)
  (doseq [namespace (#'bultitude.core/namespaces-in-jar jar-file)]
    (require namespace)
    (autoregister-transformations
     (list-hook-functions namespace))))

(defn load-hooks-dir
  "Loads all hook functions from the clj and jar files from the
  specified directory"
  [dir]
  (doseq [file (filter #'bultitude.core/clj? (file-seq (clojure.java.io/file dir)))]
    (load-hooks-file (.getAbsolutePath file)))
  (doseq [jar-file (filter #'bultitude.core/jar? (file-seq (clojure.java.io/file dir)))]
    (load-hooks-jar (.getAbsolutePath jar-file))))

(defmacro deregister-transformation
  "Wrapper to the deregister function, to ensure var conversion"
  ([function]
     `(deregister (var ~function)))
  ([hook-name function]
     `(deregister ~hook-name (var ~function))))

(defn- deregister
  "Deregisters the specified function with all hooks or deregisters the
  function with the specified hook"
  ([function]
     (doseq [hook (keys @hooked-transformations)]
       (deregister hook function)))
  ([hook-name function]
     (let [filtered-map (filter #(not (= % function)) (hook-name @hooked-transformations))]
       (if (empty? filtered-map)
         (swap! hooked-transformations dissoc hook-name)
         (swap! hooked-transformations assoc hook-name filtered-map)))
     nil))

(defn- get-hook-filename
  "Tries to find the filename associated with a given function"
  [function]
  (let [file (:file (meta function))]
    (if file
      (. (clojure.java.io/file file) getName))))

(defn loaded-hooks-files
  "Finds all filenames, from which hook functions have been loaded into
  the hooked transformations map"
  []
  (sort (distinct
         (filter #(and (not (nil? %)) (not (= "NO_SOURCE_PATH" %)))
                 (map get-hook-filename (flatten (vals @hooked-transformations)))))))

(defn loaded-hooks
  "Returns the hooked transformations map"
  []
  @hooked-transformations)
  
(defn apply-transformations
  "Applies all transformation functions associated with the given
  hook to the input"
  [hook-name input]
  (reduce #(%2 %1) input (hook-name @hooked-transformations)))