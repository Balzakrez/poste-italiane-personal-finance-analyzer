package com.financeanalyzer.service;

import com.financeanalyzer.model.Category;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class RuleBasedCategorizerService {

    private static final String FILE_REGOLE = "rules.yml";

    private final List<RegolaCategorizzazione> regole;

    /* ********************************************************************************************** */

    public RuleBasedCategorizerService() {
        this.regole = caricaRegole();
    }

    public Optional<Category> classifica(String causale) {
        if (causale == null || causale.isBlank()) {
            return Optional.empty();
        }
        String causaleNormalizzata = causale.toUpperCase();

        return regole.stream()
                .filter(regola -> causaleNormalizzata.contains(regola.pattern()))
                .map(RegolaCategorizzazione::categoria)
                .findFirst();
    }

    /* ********************************************************************************************** */

    @SuppressWarnings("unchecked")
    private List<RegolaCategorizzazione> caricaRegole() {
        try (InputStream inputStream = new ClassPathResource(FILE_REGOLE).getInputStream()) {
            Map<String, Object> radice = new Yaml().load(inputStream);
            List<Map<String, String>> voci = (List<Map<String, String>>) radice.get("regole");

            return voci.stream()
                    .map(voce -> new RegolaCategorizzazione(
                            voce.get("pattern"),
                            Category.valueOf(voce.get("categoria"))
                    ))
                    .toList();
        } catch (Exception e) {
            throw new IllegalStateException("Impossibile caricare " + FILE_REGOLE, e);
        }
    }

    private record RegolaCategorizzazione(String pattern, Category categoria) {
    }
}