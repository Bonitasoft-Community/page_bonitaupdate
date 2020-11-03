# page_bonitaupdate
Manage the Bonita Update mechanism



TODO:

Avoir un seul répertoire pour les patch. Si on a un "<patch>_uninstall.zip" alors cela signifie que le patch est installé
patch configuration : lire les valeurs par defaut d'un fichier properties locales

Installer un patch => Modifier le WAR en automatique
Date d'installation du patch sur le serveur

Historic (desinstallation, savoir la date d'install + la date de desintallation)




Politique patch privé:

1/ 2 sequencement séparés

​	Public_01, 	Public_02, 	Public_03

2/ privé

​	Verizon_01, Verizon_02

Installer les patch PUBLIC puis PRIVE

​	Corrolaire: Pour installer le patch Public_04, cela sous entends de DESINSTALLER tous les patch Privés et installer Public_04, puis RESINTALLER les patchs privés



Basculer un patch en privé

​	Si Verizon_01 est basculé en public (public_05)

- Alors Verizon_01 n'existe plus

- il faut alors, quand on install Public_05, Desinstaller Verizon_01 et le marquer en Obsolete
  * Si on desinstalle Public_05, alors il faut reinstaller Verizon_01



Jouer le uninstall avant pour verifier qu'il peut passer (un JAR peut etre vérrouillé)





CookieStore cookieStore = new BasicCookieStore();
httpContext = new BasicHttpContext();
httpContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);