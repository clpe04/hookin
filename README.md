# HookIn

Pronounced \'Hú-kin\

Is a clojure hook management system, enabling the user to manually register or 
dynamically load and register clj and jar files and extract functions marked 
as hooked transformations and add them to a list of transformations, which can 
then be applied to data.

## Usage

This section provides some short examples of HookIn and its usage.

#### Example 1 - Simple usage

    (defn
      test-function
      [data]
      (+ data 10))

    (register-transformation :add-10 test-function)

    (apply-transformations :add-10 15)

Defines a function named "test-function", which are then registered in the plugin
management system under the hook :add-10 and then applied to the number 15 for a result of 25.

#### Example 2 - Autoloading functions

##### Code placed in a file called plugin.clj

    (ns test.plugin
      (:require [hookin.core :as hookin]))

    (hookin/hookfn test-function :add-10
                   [data]
                   (+ data 10))

##### In your project

    (load-hooks-file "/path-to-file/plugin.clj")

    (apply-transformations :add-10 15)

Defines a hooked function named "test-function" associated with the hook ":add-10" in a 
seperate clj file. The file is then loaded into the project and the function registers itself 
in the hook management system under the hook :add-10 and is then applied to the number 15 
for a result of 25.

## Acknowledgements

This project is the new version of the Plugin Management System project, originally based on the ideas and initial work of [Jacob Emcken](https://github.com/jacobemcken "jacobemcken on GitHub")

## License

Copyright (C) 2013 Claus Engel-Christensen

Distributed under the Eclipse Public License, the same as Clojure.
