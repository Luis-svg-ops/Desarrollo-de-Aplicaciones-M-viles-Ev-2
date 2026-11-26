# Reglas de R8 (minificación y ofuscación del APK release).
# R8 renombra y elimina clases que "no se usan". Gson y Room leen las clases por reflexión
# (por el nombre de sus campos), así que hay que decirle a R8 que no las toque.

# Forma del JSON de los microservicios y de Open-Meteo (Gson usa los nombres de los campos)
-keep class cl.duoc.acis.data.remote.** { *; }
# Entidades de Room y enums de las reglas (estados, roles, acciones)
-keep class cl.duoc.acis.model.** { *; }
# Informe JSON de la OT (también se arma con Gson)
-keep class cl.duoc.acis.util.InformeOt { *; }
-keep class cl.duoc.acis.util.*Informe { *; }

# Retrofit necesita los tipos genéricos (List<OtDto>) y las anotaciones (@GET, @Body...)
-keepattributes Signature, *Annotation*, InnerClasses, EnclosingMethod
