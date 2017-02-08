package com.chin.ygodb;

import android.content.Context;
import android.util.Log;
import android.util.LruCache;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Parsing booster dom
 *
 * Created by Chin on 06-Feb-17.
 */
public class BoosterParser {
    private Context context;
    private Element dom;
    private String boosterName;
    private static final LruCache<String, Element> cache = new LruCache<>(5);

    public BoosterParser(Context context, String boosterName, String boosterUrl) throws IOException {
        this.context = context;
        this.boosterName = boosterName;
        this.dom = getDocument(boosterName, boosterUrl);
    }

    private static Element getDocument(String boosterName, String boosterUrl) throws IOException {
        synchronized (cache) {
            Element elem = cache.get(boosterName);
            if (elem != null) {
                return elem;
            }
            else {
                String html = Jsoup.connect("http://yugioh.wikia.com" + boosterUrl)
                        .ignoreContentType(true).execute().body();
                Document dom = Jsoup.parse(html);
                elem = dom.getElementById("mw-content-text");
                removeSupTag(elem);
                cache.put(boosterName, elem);
                return elem;
            }
        }
    }

    public String getJapaneseReleaseDate() {
        try {
            Elements rows = dom.getElementsByClass("infobox").first().getElementsByTag("tr");
            for (int i = 0; i < rows.size(); i++) {
                Element row = rows.get(i);
                if (row.text().equals("Release dates")) {
                    for (int j = i + 1; j < rows.size(); j++) {
                        Elements headers = rows.get(j).getElementsByTag("th");
                        if (headers.size() > 0 && headers.get(0).text().equals("Japanese")) {
                            String date = rows.get(j).getElementsByTag("td").first().text();
                            return date;
                        }
                    }
                }
            }
        }
        catch (Exception e) {
            Log.i("ygodb", "Unable to get Japanese release date");
        }

        return null;
    }

    public String getEnglishReleaseDate() {
        try {
            Elements rows = dom.getElementsByClass("infobox").first().getElementsByTag("tr");
            for (int i = 0; i < rows.size(); i++) {
                Element row = rows.get(i);
                if (row.text().equals("Release dates")) {
                    String date = null;
                    for (int j = i + 1; j < rows.size(); j++) {
                        Elements headers = rows.get(j).getElementsByTag("th");
                        if (headers.size() > 0) {
                            Element header = headers.get(0);

                            if (header.text().startsWith("English")) {
                                date = rows.get(j).getElementsByTag("td").first().text();
                            }

                            if (header.text().equals("English (na)")){
                                return date;
                            }
                        }
                    }

                    return date;
                }
            }
        }
        catch (Exception e) {
            Log.i("ygodb", "Unable to get Japanese release date");
        }

        return null;
    }

    public String getImageLink() {
        try {
            return dom.getElementsByClass("image-thumbnail").first().attr("href");
        }
        catch (Exception e) {
            return null;
        }
    }

    public String getIntroText() {
        try {
            return dom.select("#mw-content-text > p").first().text();
        }
        catch (Exception e) {
            return null;
        }
    }

    public String getFeatureText() {
        try {
            return dom.select("#mw-content-text > ul").first().text();
        }
        catch (Exception e) {
            return null;
        }
    }

    /**
     * Get the list of cards in this booster
     * @return list of cards
     */
    public List<Card> getCardList() {
        List<Card> cards = new ArrayList<>();
        try {
            Elements rows = dom.getElementsByClass("wikitable").first()
                    .getElementsByTag("tbody").first()
                    .getElementsByTag("tr");
            for (Element row : rows) {
                try {
                    Elements cells = row.getElementsByTag("td");
                    String setNumber = cells.get(0).text();
                    String cardName = cells.get(1).text();
                    String rarity = cells.get(2).text();

                    DatabaseQuerier querier = new DatabaseQuerier(context);
                    String criteria = "name = '" + cardName + "'";
                    List<Card> res = querier.executeQuery(criteria);

                    if (res.size() > 0) {
                        Card card = res.get(0);
                        card.setNumber = setNumber;
                        card.rarity = rarity;
                        cards.add(card);
                    }
                    else {
                        // TODO: card not in offline db, do something
                    }
                }
                catch (Exception e) {
                    // do nothing
                }
            }
        }
        catch (Exception e) {
            Log.i("ygodb", "Unable to get card list for booster: " + boosterName);
        }

        return cards;
    }

    private static void removeSupTag(Element elem) {
        Elements sups = elem.getElementsByTag("sup");
        for (Element e : sups) {
            e.remove();
        }
    }
}
