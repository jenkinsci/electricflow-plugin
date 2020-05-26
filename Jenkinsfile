buildPlugin(configurations: [
          // Sanity on the minimum required Jenkins version
         [ platform: "linux", jdk: "8", jenkins: '2.190.3' ],

          // Sanity on the recent Jenkins version
         [ platform: "linux", jdk: "8", jenkins: null ],

         // + Java 11
         [ platform: "linux", jdk: "11", jenkins: null, javaLevel: "8"],

         // + Windows
         [ platform: "windows", jdk: "11", jenkins: null, javaLevel: "8" ],
])