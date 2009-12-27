(ns appengine-clj.datastore
  (:import (com.google.appengine.api.datastore
            DatastoreServiceFactory Entity Key Query KeyFactory
            FetchOptions$Builder
            Query$FilterOperator
            Query$SortDirection))
  (:refer-clojure :exclude [get = > >= < <=]))

(def ASC Query$SortDirection/ASCENDING)
(def DESC Query$SortDirection/DESCENDING)

(def = Query$FilterOperator/EQUAL)
(def > Query$FilterOperator/GREATER_THAN)
(def >= Query$FilterOperator/GREATER_THAN_OR_EQUAL)
(def < Query$FilterOperator/LESS_THAN)
(def <= Query$FilterOperator/LESS_THAN_OR_EQUAL)

(defn entity-to-map
  "Converts an instance of com.google.appengine.api.datastore.Entity
  to a PersistentHashMap with properties stored under keyword keys,
  plus the entity's kind stored under :kind and key stored under :key."
  [#^Entity entity]
  (reduce #(assoc %1 (keyword (key %2)) (val %2))
    {:kind (.getKind entity) :key (.getKey entity)}
    (.entrySet (.getProperties entity))))

(defn map-to-entity [map]
  (reduce #(do (.setProperty %1 (name (first %2)) (second %2))
               %1)
          (Entity. (:key map))
          (dissoc map :key)))

(defn get
  "Retrieves the identified entity or raises EntityNotFoundException."
  [#^Key key]
  (entity-to-map (.get (DatastoreServiceFactory/getDatastoreService) key)))

(defn put [map]
  (.put (DatastoreServiceFactory/getDatastoreService)
        (map-to-entity map)))

(defn create-key [kind id]
  (KeyFactory/createKey kind (Long/valueOf (String/valueOf id))))

(defn key-to-str [key]
  (KeyFactory/keyToString key))

(defn str-to-key [s]
  (KeyFactory/stringToKey s))

(defn find-all
  "Executes the given com.google.appengine.api.datastore.Query
  and returns the results as a lazy sequence of items converted with entity-to-map."
  [#^Query query]
  (let [data-service (DatastoreServiceFactory/getDatastoreService)
        results (.asIterable (.prepare data-service query))]
    (map entity-to-map results)))

(defn find-limit
  [#^Query query offset limit]
  (let [data-service (DatastoreServiceFactory/getDatastoreService)
        results (.asIterable (.prepare data-service query)
                             (.offset (FetchOptions$Builder/withLimit limit)
                                      offset))]
    (map entity-to-map results)))




(defn create
  "Takes a map of keyword-value pairs and puts a new Entity in the Datastore.
  The map must include a :kind String.
  Returns the saved Entity converted with entity-to-map (which will include the assigned :key)."
  ([item] (create item nil))
  ([item #^Key parent-key]
    (let [kind (item :kind)
          properties (dissoc item :kind)
          entity (if parent-key (Entity. kind parent-key) (Entity. kind))]
      (doseq [[prop-name value] properties] (.setProperty entity (name prop-name) value))
      (.put (DatastoreServiceFactory/getDatastoreService) entity)
      (entity-to-map entity))))

(defn delete
  "Deletes the identified entities."
  [& #^Key keys]
  (.delete (DatastoreServiceFactory/getDatastoreService) keys))

