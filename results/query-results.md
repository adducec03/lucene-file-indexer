# Query results

- Generated: 2026-11-11 12:54
- Index: `index`
- Queries file: `queries.txt` (21 queries)

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
| 12 | `contenuto il the` | Solo parole comuni: senza stopword vengono trovati quasi tutti i file (BM25 le pesa poco) | 199 | it_Emily_Blunt |
| 13 | `nome example1` | Numero nel nome: example1 resta un unico termine e trova solo example1 | 1 | example1 |
| 14 | `contenuto uomo` | Elisione: "uomo" deve trovare anche "dell'uomo" e "l'uomo" | 19 | it_Belle_Gunness |
| 15 | `contenuto italia` | Elisione: "italia" deve trovare anche "l'Italia" e "dell'Italia" | 38 | it_Giuliano_Ferrara |
| 16 | `contenuto citta` | Accenti: "citta" (senza accento) deve trovare "città" | 34 | it_Citta_di_Ebla |
| 17 | `contenuto king` | Possessivo inglese: "king" deve trovare anche "king's" | 21 | it_Non_siamo_piu_quelli_di_Mi_fist |
| 18 | `contenuto era` | Parola comune italiana che è anche una parola inglese: non è più eliminata | 95 | it_Detlev_Peukert |
| 19 | `titolo knitting` | Campo sconosciuto | error | - |
| 20 | `contenuto` | Testo della query mancante | error | - |
| 21 | `contenuto "frase aperta` | Virgolette non chiuse | error | - |

## Details

### 1. `nome knitting`

*Un termine nel nome del file*

```
total number of results: 1
name: en_Knitting; score: 2.828786; position: 1
```

### 2. `nome history martial`

*Più termini nel nome (OR): basta che ne compaia uno*

```
total number of results: 1
name: en_History_of_martial_arts; score: 4.111506; position: 1
```

### 3. `nome "martial arts"`

*Frase tra virgolette nel nome*

```
total number of results: 1
name: en_History_of_martial_arts; score: 4.111506; position: 1
```

### 4. `nome list`

*Termine molto comune nei nomi: il totale supera il top-K mostrato*

```
total number of results: 6
name: en_List_of_formulae_involving; score: 1.4415342; position: 1
name: en_List_of_systems_scientists; score: 1.4415342; position: 2
name: en_List_of_neighbourhoods_in_Calgary; score: 1.3211856; position: 3
name: en_List_of_visual_novel_engines; score: 1.3211856; position: 4
name: en_List_of_Daemons_of_the_Shadow_Realm_characters; score: 1.05656; position: 5
```

### 5. `contenuto napoleone`

*Termine italiano nel contenuto*

```
total number of results: 5
name: it_Castello_di_Saint_Germain_en_Laye; score: 2.5254717; position: 1
name: it_Teatro_Grande_Brescia; score: 2.43229; position: 2
name: it_Angelina_Lanza_Damiani; score: 2.001461; position: 3
name: it_Sori; score: 1.500375; position: 4
name: it_Stato_e_rivoluzione; score: 1.3201017; position: 5
```

### 6. `contenuto earthquake`

*Termine inglese nel contenuto*

```
total number of results: 1
name: en_2020_Petrinja_earthquake; score: 4.7045727; position: 1
```

### 7. `contenuto "world war"`

*Frase nel contenuto*

```
total number of results: 16
name: en_Protectorate_of_Bohemia_and_Moravia; score: 2.247861; position: 1
name: en_1990_Daniel_Johnston_plane_crash; score: 2.1566086; position: 2
name: en_Bukittinggi; score: 2.092175; position: 3
name: en_Winsford_United_F_C; score: 2.012023; position: 4
name: en_1950s_American_automobile_culture; score: 1.9318712; position: 5
```

### 8. `contenuto guerra mondiale`

*Più termini nel contenuto (OR)*

```
total number of results: 44
name: it_Polemiko_Nautiko; score: 3.4286017; position: 1
name: it_Architettura_della_Repubblica_Democratica_Tedesca; score: 2.7889102; position: 2
name: it_Simeone_II_di_Bulgaria; score: 2.780004; position: 3
name: it_Armee_de_l_air_et_de_l_espace; score: 2.6922066; position: 4
name: it_Earle_Wheeler; score: 2.6613746; position: 5
```

### 9. `contenuto NAPOLEONE`

*Maiuscole ignorate: stesso risultato di "contenuto napoleone"*

```
total number of results: 5
name: it_Castello_di_Saint_Germain_en_Laye; score: 2.5254717; position: 1
name: it_Teatro_Grande_Brescia; score: 2.43229; position: 2
name: it_Angelina_Lanza_Damiani; score: 2.001461; position: 3
name: it_Sori; score: 1.500375; position: 4
name: it_Stato_e_rivoluzione; score: 1.3201017; position: 5
```

### 10. `contenuto "data scientist"`

*Frase presente in un solo file*

```
total number of results: 1
name: example1; score: 3.7460384; position: 1
```

### 11. `contenuto xyzqwerty`

*Termine inesistente: nessun risultato*

```
total number of results: 0
```

### 12. `contenuto il the`

*Solo parole comuni: senza stopword vengono trovati quasi tutti i file (BM25 le pesa poco)*

```
total number of results: 199
name: it_Emily_Blunt; score: 1.0205749; position: 1
name: it_Roy_Scheider; score: 1.0196627; position: 2
name: it_Osman_Ali_Khan_Asif_Jah_VII; score: 1.0154583; position: 3
name: it_Jennifer_Aniston; score: 1.0154531; position: 4
name: it_Washington_1796; score: 1.0130955; position: 5
```

### 13. `nome example1`

*Numero nel nome: example1 resta un unico termine e trova solo example1*

```
total number of results: 1
name: example1; score: 3.2341726; position: 1
```

### 14. `contenuto uomo`

*Elisione: "uomo" deve trovare anche "dell'uomo" e "l'uomo"*

```
total number of results: 19
name: it_Belle_Gunness; score: 2.1147656; position: 1
name: it_Episodi_di_CSI_Miami_ottava_stagione; score: 2.0033443; position: 2
name: it_Call_of_Duty_Black_Ops_III; score: 1.7436023; position: 3
name: it_Roy_Scheider; score: 1.6789862; position: 4
name: it_Osman_Ali_Khan_Asif_Jah_VII; score: 1.5791646; position: 5
```

### 15. `contenuto italia`

*Elisione: "italia" deve trovare anche "l'Italia" e "dell'Italia"*

```
total number of results: 38
name: it_Giuliano_Ferrara; score: 1.4793729; position: 1
name: it_Celebrante_umanista; score: 1.3388402; position: 2
name: it_San_Valentino_Torio; score: 1.3388402; position: 3
name: it_Bartsia_alpina; score: 1.3306865; position: 4
name: it_Salvatore_Camporeale; score: 1.2601285; position: 5
```

### 16. `contenuto citta`

*Accenti: "citta" (senza accento) deve trovare "città"*

```
total number of results: 34
name: it_Citta_di_Ebla; score: 1.696857; position: 1
name: it_Offensiva_russa_nel_Donbass; score: 1.6636398; position: 2
name: it_Architettura_della_Repubblica_Democratica_Tedesca; score: 1.6478025; position: 3
name: it_San_Valentino_Torio; score: 1.4850081; position: 4
name: it_Ladislao_I_di_Polonia; score: 1.4819999; position: 5
```

### 17. `contenuto king`

*Possessivo inglese: "king" deve trovare anche "king's"*

```
total number of results: 21
name: it_Non_siamo_piu_quelli_di_Mi_fist; score: 1.924665; position: 1
name: en_Agnes_World; score: 1.898685; position: 2
name: en_Spanish_Golden_Age; score: 1.8294095; position: 3
name: en_201617_RFU_Championship; score: 1.7017143; position: 4
name: en_List_of_neighbourhoods_in_Calgary; score: 1.6842446; position: 5
```

### 18. `contenuto era`

*Parola comune italiana che è anche una parola inglese: non è più eliminata*

```
total number of results: 95
name: it_Detlev_Peukert; score: 0.718568; position: 1
name: it_Progetto_Ophelia; score: 0.7135701; position: 2
name: it_Belle_Gunness; score: 0.709772; position: 3
name: it_Caproni_Ca_16; score: 0.6963236; position: 4
name: it_Nineteenth_Air_Force; score: 0.6941275; position: 5
```

### 19. `titolo knitting`

*Campo sconosciuto*

```
Error: unknown field, use 'nome' or 'contenuto'
```

### 20. `contenuto`

*Testo della query mancante*

```
Error: missing query text after 'contenuto'
```

### 21. `contenuto "frase aperta`

*Virgolette non chiuse*

```
Error: invalid syntax: Cannot parse '"frase aperta': Lexical error at line 1, column 14.  Encountered: <EOF> after prefix "\"frase aperta" (in lexical state 2)
```

