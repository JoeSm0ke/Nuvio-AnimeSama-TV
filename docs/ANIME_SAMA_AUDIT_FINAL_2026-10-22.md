# Anime-Sama TV / Qilive — rapport final d'audit sur PC
Date : 22 octobre 2026.
Périmètre : fork Android com.nuviodebug.com, fournisseur Anime-Sama 1.1.61, catalogue public et APK ARM local.
**Aucun accès à la Qilive, aucune installation et aucune modification de ses données.**

## Décision
Les audits de code, données, flux et distribution faisables sur PC sont terminés. L'application n'est PAS validée matériellement sur Android TV. Migration BLOQUÉE : ancienne version GitHub et nouvel APK local ne sont pas signés avec le même certificat ; ne pas désinstaller ou installer sans sauvegarde et accord spécifique.

## Catalogue, catégories et affiches
- Recherche : 234 séries, 234 IDs uniques, aucune entrée movie et aucun poster manquant.
- Les classiques : 24 séries ; titres récents : 64 ; nouveautés : 60.
- Chaque série de ces trois catégories est présente dans la recherche ; mêmes ID, titre, poster et posterShape.
- Les 234 liens d'affiches sont HTTPS raw.githubusercontent.com/Anime-Sama/IMG ; aucun doublon d'affiche ni URL d'un autre domaine.
- Échantillon déterministe de huit affiches distribuées dans l'index : 8/8 HTTP 200 et image/jpeg. Les autres URL n'ont pas toutes été interrogées.
- Audit hors ligne reproductible dans le dépôt fournisseur : npm run audit:catalog. Test live des sources : npm run smoke:anime-sama.

## Application, navigation et personnalisation
- « Ma liste » : projection non destructive de LibraryRepository par profil, maximum 18 séries, tri par récence, sans nouvelle base de favoris.
- Protection contre une réponse tardive de catalogue après changement de profil.
- Accueils classique et moderne : restauration du focus par clé stable, attente du catalogue mémorisé s'il charge et repli si disparu.
- En-tête « Ma liste » sans suffixe générique ; suppression du faux bouton « Voir tout » sans destination.
- Accueil grille : jusqu'à 18 affiches de « Ma liste » visibles, aucun faux bouton « Voir tout » ; vraies catégories inchangées.
- Chargement des images : composants Nuvio Coil/AsyncImage, placeholders, dimensionnement des requêtes, cache mémoire et gestion d'échec. Aucun second moteur de cache introduit. Fluidité et consommation mémoire non mesurées sur la TV.

## Lecture et reprise
- Groupes de continuité séparés pour les sources explicitement VF et VOSTFR ; code générique fr ne fait pas supposer un doublage.
- Cache de dernier lien : évite les URL HTTP Anime-Sama possiblement expirées, y compris les anciennes entrées. Aucune altération du suivi de progression.
- Bouton manuel « Changer de source » après erreur Anime-Sama : relance la découverte, sans changement de langue automatique.
- Résolutions PC : Dan Da Dan S1E1 2 sources ; Naruto S1E1 4 ; Bloom S1E1 4. 3/3 cas réussis. Ne démontre pas le décodage audio/vidéo Android TV.
- Reprise, historique, sous-titres et gestion des épisodes existants Nuvio conservés.

## Tests et livrable
- Campagne ciblée Gradle : **36/36 tests réussis** ; 4 recherche, 6 favoris, 6 focus, 3 relocalisation d'identité, 4 cache source, 4 continuité VF/VOSTFR, 9 sélection de sources.
- Construction ARM : **BUILD SUCCESSFUL**. Fichier sur le PC :
  %LOCALAPPDATA%\QiliveRemote\Builds\AnimeSama-LocalStable\Anime-Sama-TV-Qilive-ARM.apk
- Taille : 91 764 926 octets ; SHA-256 APK : E78A041AAAD9D208414442BB7DF65264019D12C3A4AA62425A8BF8F0867D3AD1
- Certificat APK local SHA-256 : f3177306ef22d9686885e46d7fd224a1efceb328f775e1b066fba787e1fcf582
- Copie de l'APK précédent : Builds\AnimeSama-LocalStable\Anime-Sama-TV-before-final-audit-20261022.apk
- Avertissements D8 de répartition des classes entre DEX : non bloquants ici, à surveiller si problème de démarrage sur appareil.

## GitHub et distribution
- Client : https://github.com/JoeSm0ke/Nuvio-AnimeSama-TV/tree/anime-sama-tv
- Fournisseur : https://github.com/JoeSm0ke/Nuvio-AnimeSama-Qilive
- Fournisseur public : manifest 1.1.61 et recherche 234 fiches.
- Préflight local sans ADB dans le dépôt client : scripts/Preflight-Anime-Sama-TV.ps1 -Online. Vérifie révisions, signature, hash APK, provider et catalogue publics ; avertit sur l'incompatibilité historique des certificats.
- Pas de keystore de signature Android ni d'APK privé ajouté aux commits de ce projet.

## Reste impérativement pour la validation réelle, uniquement sur autorisation
1. Examiner la version et signature réellement installées, puis sauvegarder les données TV.
2. Définir la migration tenant compte des certificats incompatibles ; aucune désinstallation sans accord.
3. Tester les flèches, OK, Retour, changement de profil, rendu des affiches, accès Ma liste et stabilité du focus dans les trois accueils.
4. Tester décodage image/son, sous-titres, VF/VOSTFR, reprise, source 403 expirée et changement d'hébergeur.
5. Mesurer RAM, fluidité, réseau et endurance pour vérifier l'absence de crash mémoire.

**Conclusion : audits PC clôturés. Aucun test matériel TV ni migration revendiqués.**
