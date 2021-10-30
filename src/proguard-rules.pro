# Add any ProGuard configurations specific to this
# extension here.

-keep public class com.oseamiya.inappbilling.InAppBilling {
    public *;
 }
-keeppackagenames gnu.kawa**, gnu.expr**

-optimizationpasses 4
-allowaccessmodification
-mergeinterfacesaggressively

-repackageclasses 'com/oseamiya/inappbilling/repack'
-flattenpackagehierarchy
-dontpreverify
