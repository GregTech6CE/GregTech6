# Original material-role fixture

`SharedMaterialBootstrapGameTests.java` is frozen from commit `216cb6f9c`, before the runtime fixture was updated for accepted CE source restorations and the optional addon sample. Its original 1156-object / 109-prefix assertions and role SHA-256 remain intact for `verify-material-roles.py original` and `OriginalMaterialRoleProbe`.

The live NeoForge fixture under `multiversion/versions/1.21.1-neoforge` checks the adopted 1160-object / 132-prefix core catalog. It reproduces the same original role fingerprint by excluding only the four independently audited restored woods and normalizing only PetrifiedWood's restored STONE flag. Those five changes are also asserted directly before normalization. Addon-owned material rows are excluded from the core fingerprint.

The original mode still requires the preserved source-snapshot Git ref recorded in `core/provenance/material-role-extractions.json`; this fixture does not replace missing source history or constitute a new original-source run.
