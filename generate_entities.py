import os
import re

os.makedirs("src/main/java/com/carthage/entity/enums", exist_ok=True)

enums = {
    "AccountStatus": ["ACTIVE", "BANNED", "PENDING"],
    "GameStatus": ["ACTIVE", "INACTIVE"],
    "GameType": ["FPS", "MOBA", "RPG", "SPORTS", "OTHER"],
    "MatchStatus": ["SCHEDULED", "IN_PROGRESS", "COMPLETED", "CANCELLED"],
    "ReclamationCategory": ["TECHNICAL", "BILLING", "FEEDBACK", "OTHER"],
    "ReclamationPriority": ["LOW", "MEDIUM", "HIGH", "URGENT"],
    "ReclamationStatus": ["PENDING", "OPEN", "CLOSED", "RESOLVED"],
    "SkinRarity": ["COMMON", "RARE", "EPIC", "LEGENDARY"],
    "SkinType": ["DIGITAL", "PHYSICAL"],
    "TeamRole": ["MEMBER", "CAPTAIN", "COACH"],
    "TeamStatus": ["ACTIVE", "INACTIVE", "DISBANDED"],
    "TournamentStatus": ["UPCOMING", "ONGOING", "COMPLETED"],
    "TournamentType": ["SINGLE_ELIMINATION", "DOUBLE_ELIMINATION", "ROUND_ROBIN"]
}

for name, values in enums.items():
    with open(f"src/main/java/com/carthage/entity/enums/{name}.java", "w", encoding="utf-8") as f:
        f.write(f"package com.carthage.entity.enums;\n\npublic enum {name} {{\n    " + ",\n    ".join(values) + "\n}\n")

entities = {
    "User": [
        ("id", "UUID"), ("email", "String"), ("username", "String"), ("nickname", "String"),
        ("password", "String"), ("roles", "List<String>"), ("balance", "int"),
        ("status", "AccountStatus"), ("isVerified", "boolean"), ("discordId", "String"),
        ("createdAt", "LocalDateTime"), ("license", "License"), ("profile", "Profile"),
        ("authToken", "AuthToken"), ("teamMemberships", "List<TeamMembership>"), ("purchases", "List<Purchase>")
    ],
    "AuthToken": [
        ("id", "UUID"), ("value", "String"), ("expiresAt", "LocalDateTime"),
        ("createdAt", "LocalDateTime"), ("user", "User")
    ],
    "PasswordResetToken": [
        ("id", "UUID"), ("token", "String"), ("expiresAt", "LocalDateTime"),
        ("createdAt", "LocalDateTime"), ("user", "User")
    ],
    "Profile": [
        ("id", "UUID"), ("bio", "String"), ("avatarUrl", "String"),
        ("createdAt", "LocalDateTime"), ("updatedAt", "LocalDateTime"), ("user", "User")
    ],
    "License": [
        ("id", "UUID"), ("licenseCode", "String"), ("isUsed", "boolean"),
        ("usedAt", "LocalDateTime"), ("createdAt", "LocalDateTime"), ("assignedTo", "User")
    ],
    "Team": [
        ("id", "UUID"), ("name", "String"), ("tag", "String"), ("description", "String"),
        ("status", "TeamStatus"), ("inviteCode", "String"), ("createdAt", "LocalDateTime"),
        ("captain", "User"), ("members", "List<TeamMembership>")
    ],
    "TeamMembership": [
        ("id", "UUID"), ("role", "TeamRole"), ("joinedAt", "LocalDateTime"),
        ("team", "Team"), ("player", "User")
    ],
    "Game": [
        ("id", "UUID"), ("name", "String"), ("description", "String"), ("type", "GameType"),
        ("status", "GameStatus"), ("imageUrl", "String"), ("createdAt", "LocalDateTime"),
        ("tournois", "List<Tournoi>"), ("skins", "List<Skin>")
    ],
    "Tournoi": [
        ("id", "UUID"), ("nom", "String"), ("dateDebut", "LocalDateTime"), ("dateFin", "LocalDateTime"),
        ("nbEquipesMax", "int"), ("prizePool", "int"), ("status", "TournamentStatus"),
        ("type", "TournamentType"), ("place", "String"), ("createdAt", "LocalDateTime"),
        ("updatedAt", "LocalDateTime"), ("game", "Game"), ("teams", "List<Team>"),
        ("matches", "List<MatchEntity>"), ("winner", "Team"), ("referee", "User")
    ],
    "MatchEntity": [
        ("id", "UUID"), ("round", "int"), ("status", "MatchStatus"), ("scheduledAt", "LocalDateTime"),
        ("startedAt", "LocalDateTime"), ("completedAt", "LocalDateTime"), ("score", "String"),
        ("createdAt", "LocalDateTime"), ("updatedAt", "LocalDateTime"), ("tournoi", "Tournoi"),
        ("team1", "Team"), ("team2", "Team"), ("winner", "Team")
    ],
    "Merch": [
        ("id", "UUID"), ("name", "String"), ("description", "String"), ("price", "int"),
        ("stock", "int"), ("imageUrl", "String"), ("type", "String"), ("createdAt", "LocalDateTime"),
        ("game", "Game"), ("purchases", "List<Purchase>")
    ],
    "Skin": [
        ("id", "UUID"), ("name", "String"), ("description", "String"), ("imageUrl", "String"),
        ("price", "int"), ("rarity", "SkinRarity"), ("skinType", "SkinType"), ("stock", "int"),
        ("apiProvider", "String"), ("deliveryMethod", "String"), ("createdAt", "LocalDateTime"),
        ("game", "Game")
    ],
    "UserSkin": [
        ("id", "UUID"), ("purchasedAt", "LocalDateTime"), ("status", "String"),
        ("user", "User"), ("skin", "Skin")
    ],
    "Purchase": [
        ("id", "UUID"), ("quantity", "int"), ("totalPrice", "int"), ("purchaseDate", "LocalDateTime"),
        ("merch", "Merch"), ("user", "User")
    ],
    "Product": [
        ("id", "Integer"), ("productType", "String"), ("name", "String"), ("description", "String"),
        ("pricePoints", "int"), ("available", "boolean")
    ],
    "Reclamation": [
        ("id", "UUID"), ("subject", "String"), ("message", "String"), ("category", "ReclamationCategory"),
        ("priority", "ReclamationPriority"), ("status", "ReclamationStatus"), ("createdAt", "LocalDateTime"),
        ("updatedAt", "LocalDateTime"), ("author", "User"), ("responses", "List<ReclamationResponse>")
    ],
    "ReclamationResponse": [
        ("id", "UUID"), ("message", "String"), ("createdAt", "LocalDateTime"), ("isAdminResponse", "boolean"),
        ("reclamation", "Reclamation"), ("author", "User")
    ]
}

def get_imports(fields):
    imports = set()
    imports.add("java.util.Objects")
    for _, t in fields:
        if "UUID" in t: imports.add("java.util.UUID")
        if "LocalDateTime" in t: imports.add("java.time.LocalDateTime")
        if "List<" in t: imports.add("java.util.List")
        # Check for enums
        for e in enums.keys():
            if e in t:
                imports.add(f"com.carthage.entity.enums.{e}")
    return sorted(list(imports))

def capitalize(s):
    return s[0].upper() + s[1:]

for class_name, fields in entities.items():
    imports = get_imports(fields)
    with open(f"src/main/java/com/carthage/entity/{class_name}.java", "w", encoding="utf-8") as f:
        f.write("package com.carthage.entity;\n\n")
        for imp in imports:
            f.write(f"import {imp};\n")
        if imports:
            f.write("\n")
        
        f.write(f"public class {class_name} {{\n\n")
        
        for name, t in fields:
            f.write(f"    private {t} {name};\n")
        f.write("\n")
        
        # no-args constructor
        f.write(f"    public {class_name}() {{}}\n\n")
        
        # all-args constructor
        params = ", ".join([f"{t} {name}" for name, t in fields])
        f.write(f"    public {class_name}({params}) {{\n")
        for name, _ in fields:
            f.write(f"        this.{name} = {name};\n")
        f.write("    }\n\n")
        
        # getters/setters
        for name, t in fields:
            prefix = "is" if t == "boolean" else "get"
            f.write(f"    public {t} {prefix}{capitalize(name)}() {{\n")
            f.write(f"        return {name};\n")
            f.write("    }\n\n")
            
            f.write(f"    public void set{capitalize(name)}({t} {name}) {{\n")
            f.write(f"        this.{name} = {name};\n")
            f.write("    }\n\n")

        f.write("    @Override\n")
        f.write(f"    public boolean equals(Object o) {{\n")
        f.write(f"        if (this == o) return true;\n")
        f.write(f"        if (o == null || getClass() != o.getClass()) return false;\n")
        f.write(f"        {class_name} that = ({class_name}) o;\n")
        
        equals_checks = []
        for name, t in fields:
            if t in ["int", "boolean"]:
                equals_checks.append(f"{name} == that.{name}")
            else:
                equals_checks.append(f"Objects.equals({name}, that.{name})")
        
        if not equals_checks:
            f.write("        return true;\n")
        else:
            f.write("        return " + " &&\n                ".join(equals_checks) + ";\n")
        f.write("    }\n\n")
        
        f.write("    @Override\n")
        f.write(f"    public int hashCode() {{\n")
        hash_fields = ", ".join([name for name, _ in fields])
        f.write(f"        return Objects.hash({hash_fields});\n")
        f.write("    }\n")
            
        f.write("}\n")
