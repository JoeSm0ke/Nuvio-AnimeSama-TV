# Qilive Ezio — test TV et correction lecture
Date : 23 octobre 2026.

## Appareil et installation
- Qilive UHD Android TV (Android 11), connexion ADB autorisée par l'utilisateur (réseau local 192.168.1.35:5555).
- Deux anciennes installations détectées : com.nuvio.tv et com.nuviodebug.com. APK d'origine sauvegardés sur le PC dans %LOCALAPPDATA%\\QiliveRemote\\TV-Backup\\Ezio-Qilive-20261023-PreMigration. Données privées des anciennes applications non exportables via run-as (non debuggable).
- Les deux anciennes applications ont été désinstallées à la demande de l'utilisateur. Installation neuve du dernier APK audité : com.nuviodebug.com. Une première installation ADB streaming a perdu la connexion mais le package a finalement été installé. Lancement de l'application en mode sans compte, avancé, accueil moderne.
- Dernier APK correctif : %LOCALAPPDATA%\\QiliveRemote\\Builds\\AnimeSama-LocalStable\\Anime-Sama-TV-Qilive-ARM.apk. Mise à jour sans désinstallation, certificat local conservé.
- SHA-256 de l'APK corrigé installé sur la TV et comparé au PC : 989145ED6F88F559AE0EFB5248B4EDFD00F60C0F2FE92CA8A73002F0F45F1956. Empreintes identiques.

## Bug reproduit
- Sur une installation propre, écran de détails Dragon Ball Z indiquait « Playback unavailable » pour tous les titres malgré catalogues visibles.
- Cause vérifiée : bootstrap du plugin Anime-Sama seulement appelé au début d'une requête de flux dans StreamRepositoryImpl.captureSourceConfiguration, alors que PlaybackAvailability.canStream bloque la navigation avant la requête s'il n'existe aucun scraper installé (dépendance circulaire).
- Correctif : PlaybackAvailabilityViewModel provisionne le fournisseur à son initialisation pour le seul package dédié com.nuviodebug.com, sans modifier le Nuvio original. Le mécanisme existant de PluginManager.ensureAnimeSamaRepositoryForFork() est réutilisé, avec protection contre exceptions.
- Validation TV après mise à jour : journal PluginManager « NuvioTV repository added: Anime-Sama TV - Qilive with 1 scrapers », puis « Anime-Sama default provider initialized for dedicated fork ». Dragon Ball Z affiche « Regarder S1 E1 » au lieu de « Playback unavailable ». La sélection des sources donne VOSTFR 1080p, VOSTFR 720p, VF 480p. Recherche en ligne du fournisseur opérationnelle.

## Règles de lancement
- Sélection d'épisode : essai automatique si VF 1080p explicitement disponible et URL HTTPS lisible ; si cette combinaison n'existe pas, afficher les sources sans sélection arbitraire.
- Reprise depuis la rangée « Continuer » de l'accueil : choisir automatiquement VF de meilleure qualité disponible, ou à défaut la meilleure source existante. Reprise de progression gérée par les mécanismes natifs Nuvio. Un choix manuel demandé explicitement continue de primer.
- Exemple PC avant installation : Naruto S1E1 ne proposait que VF/VOSTFR 720p/360p, d'où le besoin d'un repli visible.
- 15 tests ciblés validés : AnimeSamaPreferredStreamTest 5, PlaybackAvailabilityViewModelTest 1, StreamAutoPlaySelectorTest 9. APK correctif BUILD SUCCESSFUL.

## Limites vérifiées
- Aucun décodage complet d'épisode confirmé durant cette session : la capture finale obtenue montrait la bande-annonce Naruto. Ne pas confondre la bande-annonce avec la lecture d'un épisode.
- Lancement automatique depuis l'accueil non testé sur un progrès réel enregistré dans cette nouvelle installation.
- Réglages de vitesse, sous-titres, VF/VOSTFR et stabilité longue durée restent à examiner avec l'utilisateur sur la Qilive.

## Sauvegardes et dépôt
- Les anciens APK restent sur le PC ; ne pas supprimer ces sauvegardes.
- Un APK antérieur au correctif est sauvegardé sous Anime-Sama-TV-before-playback-bootstrap-20261023.apk.
- Aucune clé de signature ni APK poussé vers GitHub, seuls les sources et tests.
