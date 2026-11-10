# Query results

- Generated: 2026-11-10 10:51
- Index: `index`
- Queries file: `queries.txt` (16 queries)

## Summary

| # | Query | Description | Results | Top result |
|---|---|---|---|---|
| 1 | `nome knitting` | Un termine nel nome del file | 1 | en_Knitting |
| 2 | `nome history martial` | Più termini nel nome (OR): basta che ne compaia uno | 1 | en_History_of_martial_arts |
| 3 | `nome "martial arts"` | Frase tra virgolette nel nome | 1 | en_History_of_martial_arts |
| 4 | `nome list` | Termine molto comune nei nomi: il totale supera il top-K mostrato | 6 | en_List_of_formulae_involving |
| 5 | `contenuto napoleone` | Termine italiano nel contenuto | 5 | it_Castello_di_Saint_Germain_en_Laye |
| 6 | `contenuto earthquake` | Termine inglese nel contenuto | 1 | en_2020_Petrinja_earthquake |
| 7 | `contenuto "world war"` | Frase nel contenuto | 16 | en_Protectorate_of_Bohemia_and_Moravia |
| 8 | `contenuto guerra mondiale` | Più termini nel contenuto (OR) | 44 | it_Polemiko_Nautiko |
| 9 | `contenuto NAPOLEONE` | Maiuscole ignorate: stesso risultato di "contenuto napoleone" | 5 | it_Castello_di_Saint_Germain_en_Laye |
| 10 | `contenuto "data scientist"` | Frase presente in un solo file | 1 | example1 |
| 11 | `contenuto xyzqwerty` | Termine inesistente: nessun risultato | 0 | - |
| 12 | `contenuto il the` | Solo stopword: nessun termine utile | no useful terms | - |
| 13 | `nome example1` | Numero nel nome: l'analyzer separa example e 1 | 4 | example1 |
| 14 | `titolo knitting` | Campo sconosciuto | error | - |
| 15 | `contenuto` | Testo della query mancante | error | - |
| 16 | `contenuto "frase aperta` | Virgolette non chiuse | error | - |

## Details

### 1. `nome knitting`

*Un termine nel nome del file*

```
total number of results: 1
name: en_Knitting; score: 2.834687; position: 1
```

### 2. `nome history martial`

*Più termini nel nome (OR): basta che ne compaia uno*

```
total number of results: 1
name: en_History_of_martial_arts; score: 4.1271143; position: 1
```

### 3. `nome "martial arts"`

*Frase tra virgolette nel nome*

```
total number of results: 1
name: en_History_of_martial_arts; score: 4.1271143; position: 1
```

### 4. `nome list`

*Termine molto comune nei nomi: il totale supera il top-K mostrato*

```
total number of results: 6
name: en_List_of_formulae_involving; score: 1.4470066; position: 1
name: en_List_of_systems_scientists; score: 1.4470066; position: 2
name: en_List_of_neighbourhoods_in_Calgary; score: 1.326704; position: 3
name: en_List_of_visual_novel_engines; score: 1.326704; position: 4
name: en_List_of_Daemons_of_the_Shadow_Realm_characters; score: 1.0618584; position: 5
```

### 5. `contenuto napoleone`

*Termine italiano nel contenuto*

```
total number of results: 5
name: it_Castello_di_Saint_Germain_en_Laye; score: 2.575616; position: 1
name: it_Teatro_Grande_Brescia; score: 2.4346242; position: 2
name: it_Angelina_Lanza_Damiani; score: 1.9583696; position: 3
name: it_Sori; score: 1.5225866; position: 4
name: it_Stato_e_rivoluzione; score: 1.3807766; position: 5
```

### 6. `contenuto earthquake`

*Termine inglese nel contenuto*

```
total number of results: 1
name: en_2020_Petrinja_earthquake; score: 4.6863585; position: 1
```

### 7. `contenuto "world war"`

*Frase nel contenuto*

```
total number of results: 16
name: en_Protectorate_of_Bohemia_and_Moravia; score: 2.2515166; position: 1
name: en_1990_Daniel_Johnston_plane_crash; score: 2.145143; position: 2
name: en_Bukittinggi; score: 2.0821168; position: 3
name: en_Winsford_United_F_C; score: 1.9665582; position: 4
name: en_1950s_American_automobile_culture; score: 1.9366002; position: 5
```

### 8. `contenuto guerra mondiale`

*Più termini nel contenuto (OR)*

```
total number of results: 44
name: it_Polemiko_Nautiko; score: 3.4362242; position: 1
name: it_Architettura_della_Repubblica_Democratica_Tedesca; score: 2.781188; position: 2
name: it_Simeone_II_di_Bulgaria; score: 2.7461226; position: 3
name: it_Earle_Wheeler; score: 2.700056; position: 4
name: it_Armee_de_l_air_et_de_l_espace; score: 2.5638604; position: 5
```

### 9. `contenuto NAPOLEONE`

*Maiuscole ignorate: stesso risultato di "contenuto napoleone"*

```
total number of results: 5
name: it_Castello_di_Saint_Germain_en_Laye; score: 2.575616; position: 1
name: it_Teatro_Grande_Brescia; score: 2.4346242; position: 2
name: it_Angelina_Lanza_Damiani; score: 1.9583696; position: 3
name: it_Sori; score: 1.5225866; position: 4
name: it_Stato_e_rivoluzione; score: 1.3807766; position: 5
```

### 10. `contenuto "data scientist"`

*Frase presente in un solo file*

```
total number of results: 1
name: example1; score: 3.7480755; position: 1
```

### 11. `contenuto xyzqwerty`

*Termine inesistente: nessun risultato*

```
total number of results: 0
```

### 12. `contenuto il the`

*Solo stopword: nessun termine utile*

```
no useful term in the query
```

### 13. `nome example1`

*Numero nel nome: l'analyzer separa example e 1*

```
total number of results: 4
name: example1; score: 4.8849287; position: 1
name: example2; score: 2.5396366; position: 2
name: en_2025_GN1; score: 1.8775456; position: 3
name: it_De_Havilland_Canada_DHC_1_Chipmunk; score: 1.4451988; position: 4
```

### 14. `titolo knitting`

*Campo sconosciuto*

```
Error: unknown field, use 'nome' or 'contenuto'
```

### 15. `contenuto`

*Testo della query mancante*

```
Error: missing query text after 'contenuto'
```

### 16. `contenuto "frase aperta`

*Virgolette non chiuse*

```
Error: invalid syntax: Cannot parse '"frase aperta': Lexical error at line 1, column 14.  Encountered: <EOF> after prefix "\"frase aperta" (in lexical state 2)
```

