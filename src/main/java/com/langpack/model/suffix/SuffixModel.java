package com.langpack.model.suffix;

import java.util.HashMap;

public class SuffixModel {

	static public HashMap<String, SuffixModelTemplate> suffixMap = null;

	public static void Initialize() {
		suffixMap = new HashMap<String, SuffixModelTemplate>();
		suffixMap.put("A3sg", new SuffixModelTemplate("A3sg", "ThirdPersonSingular", "Üçüncü Tekil Şahıs Eki (o)",
				SuffixType.INFLEXIONAL));
		suffixMap.put("Noun", new SuffixModelTemplate("Noun", "Noun", "İsim", SuffixType.INFLEXIONAL));
		suffixMap.put("Verb", new SuffixModelTemplate("Verb", "Verb", "Fiil", SuffixType.INFLEXIONAL));
		suffixMap.put("Adj", new SuffixModelTemplate("Adj", "Adjective", "Sıfat", SuffixType.INFLEXIONAL));
		suffixMap.put("Inf1",
				new SuffixModelTemplate("Inf1", "Infinitive1", "Mastar (mak, mek)", SuffixType.INFLEXIONAL));
		suffixMap.put("P3sg", new SuffixModelTemplate("P3sg", "ThirdPersonSingularPossessive",
				"Üçüncü Tekil Şahıs İyelik Eki (onun)", SuffixType.INFLEXIONAL));
		suffixMap.put("Acc",
				new SuffixModelTemplate("Acc", "Accusative", "Belirtme Durumu (i,e,u,ü)", SuffixType.INFLEXIONAL));
		suffixMap.put("Imp", new SuffixModelTemplate("Imp", "Imparative", "Emir Kipi", SuffixType.INFLEXIONAL));
		suffixMap.put("A2sg", new SuffixModelTemplate("A2sg", "SecondPersonSingular", "İkinci Tekil Şahıs Eki (sen)",
				SuffixType.INFLEXIONAL));
		suffixMap.put("Zero",
				new SuffixModelTemplate("Zero", "Zero", "Eksiltili Yapı/Sıfır Ek", SuffixType.INFLEXIONAL));
		suffixMap.put("Inf2",
				new SuffixModelTemplate("Inf2", "Infinitive2", "Mastar (ma, me)", SuffixType.DERIVATIONAL));
		suffixMap.put("Neg", new SuffixModelTemplate("Neg", "Negative", "Olumsuzluk (me, ma)", SuffixType.INFLEXIONAL));
		suffixMap.put("Ness", new SuffixModelTemplate("Ness", "Ness", "İsim Yapma Eki (lık)", SuffixType.DERIVATIONAL));
		suffixMap.put("Caus",
				new SuffixModelTemplate("Caus", "Causative", "Ettirgenlik (t, dır)", SuffixType.DERIVATIONAL));
		suffixMap.put("Dat",
				new SuffixModelTemplate("Dat", "Dative", "Yönelme durumu (ya, na)", SuffixType.INFLEXIONAL));
		suffixMap.put("P2sg", new SuffixModelTemplate("P2sg", "SecondPersonSingularPossessive",
				"İkinci Tekil Şahıs İyelik Eki (senin)", SuffixType.INFLEXIONAL));
		suffixMap.put("Pass",
				new SuffixModelTemplate("Pass", "Passive", "Edilgenlik (ıl, ın, nıl)", SuffixType.DERIVATIONAL));
		suffixMap.put("Become",
				new SuffixModelTemplate("Become", "Become", "Dönüşüm Eki (laş, leş)", SuffixType.DERIVATIONAL));
		suffixMap.put("With",
				new SuffixModelTemplate("With", "With", "Varlık/ Mensupluk Eki (lı, li)", SuffixType.DERIVATIONAL));
		suffixMap.put("Agt", new SuffixModelTemplate("Agt", "Agentive", "Eylem Yapan (cı, ci, cu, cü, ken)",
				SuffixType.DERIVATIONAL));
		suffixMap.put("Acquire",
				new SuffixModelTemplate("Acquire", "Acquire", "Edinme (len, lan)", SuffixType.DERIVATIONAL));
		suffixMap.put("Adv", new SuffixModelTemplate("Adv", "Adverb", "Zarf", SuffixType.INFLEXIONAL));
		suffixMap.put("Able",
				new SuffixModelTemplate("Able", "Ability", "Yeterlilik (ebil, abil)", SuffixType.INFLEXIONAL));
		suffixMap.put("Without", new SuffixModelTemplate("Without", "Without", "Yokluk/Eksiklik (sız, siz, suz, süz)",
				SuffixType.DERIVATIONAL));
		suffixMap.put("A3pl", new SuffixModelTemplate("A3pl", "ThirdPersonPlural", "Üçüncü Çoğul Şahıs Eki (onlar)",
				SuffixType.INFLEXIONAL));
		suffixMap.put("P1sg", new SuffixModelTemplate("P1sg", "FirstPersonSingularPossessive",
				"Birinci Tekil Şahıs İyelik Eki (benim)", SuffixType.INFLEXIONAL));
		suffixMap.put("Inf3",
				new SuffixModelTemplate("Inf3", "Infinitive3", "Mastar (iş, ış, uş, üş)", SuffixType.DERIVATIONAL));
		suffixMap.put("Pres",
				new SuffixModelTemplate("Pres", "PresentTense", "Geniş Zaman  (ır, ar, z) ", SuffixType.INFLEXIONAL));
		suffixMap.put("Loc",
				new SuffixModelTemplate("Loc", "Locative", "Bulunma Durumu (de, da)", SuffixType.INFLEXIONAL));
		suffixMap.put("Abl",
				new SuffixModelTemplate("Abl", "Ablative", "Ayrılma Durumu (den, dan)", SuffixType.INFLEXIONAL));
		suffixMap.put("Gen",
				new SuffixModelTemplate("Gen", "Genitive", "Tamlama (in, ın, un, ün)", SuffixType.INFLEXIONAL));
		suffixMap.put("Num", new SuffixModelTemplate("Num", "Numeral", "Sayı", SuffixType.INFLEXIONAL));
		suffixMap.put("Postp", new SuffixModelTemplate("Postp", "PostPositive", "Edat", SuffixType.INFLEXIONAL));
		suffixMap.put("Hastily",
				new SuffixModelTemplate("Hastily", "Hastily", "Tezlik (iver)", SuffixType.DERIVATIONAL));
		suffixMap.put("Aor",
				new SuffixModelTemplate("Aor", "Aorist", "Belirsiz Zaman (ır, ar, z) ", SuffixType.INFLEXIONAL));
		suffixMap.put("AorPart", new SuffixModelTemplate("AorPart", "AoristParticiple",
				"Sıfat-Fiil Eki (ar, er, ır, ir, ur, ür)", SuffixType.DERIVATIONAL));
		suffixMap.put("Opt", new SuffixModelTemplate("Opt", "Optative", "İstek Kipi (e, a)", SuffixType.DERIVATIONAL));
		suffixMap.put("A1sg", new SuffixModelTemplate("A1sg", "FirstPersonSingular", "Birinci Tekil Şahıs Eki (ben)",
				SuffixType.INFLEXIONAL));
		suffixMap.put("Equ",
				new SuffixModelTemplate("Equ", "Equ", "Eşitlik Durumu / Gibi (ce, ca)", SuffixType.DERIVATIONAL));
		suffixMap.put("Interj", new SuffixModelTemplate("Interj", "Interjection", "Ünlem", SuffixType.INFLEXIONAL));
		suffixMap.put("P3pl", new SuffixModelTemplate("P3pl", "ThirdPersonPluralPossessive",
				"Üçüncü Çoğul Şahıs İyelik Eki (onların)", SuffixType.INFLEXIONAL));
		suffixMap.put("PresPart", new SuffixModelTemplate("PresPart", "PresentParticiple", "Sıfat-Fiil Eki (an, en)",
				SuffixType.DERIVATIONAL));
		suffixMap.put("Pron", new SuffixModelTemplate("Pron", "Pronoun", "Zamir", SuffixType.INFLEXIONAL));
		suffixMap.put("AsIf",
				new SuffixModelTemplate("AsIf", "AsIf", "Kıyaslama Ulaç Eki (casına, cesine)", SuffixType.INFLEXIONAL));
		suffixMap.put("Ly",
				new SuffixModelTemplate("Ly", "Ly", "Sıfattan Zarf Yapan Ek (ca, ce)", SuffixType.DERIVATIONAL));
		suffixMap.put("Det", new SuffixModelTemplate("Det", "Determiner", "Genel Belirtici (her, hiç, bütün)",
				SuffixType.INFLEXIONAL));
		suffixMap.put("Past",
				new SuffixModelTemplate("Past", "PastTense", "Geçmiş Zaman (di)", SuffixType.INFLEXIONAL));
		suffixMap.put("Related",
				new SuffixModelTemplate("Related", "Related", "İlişkili (sal, sel, k)", SuffixType.DERIVATIONAL));
		suffixMap.put("Conj", new SuffixModelTemplate("Conj", "Conjunction", "Bağlaç", SuffixType.INFLEXIONAL));
		suffixMap.put("Ins",
				new SuffixModelTemplate("Ins", "Instrumental", "Birliktelik (la, le)", SuffixType.DERIVATIONAL));
		suffixMap.put("JustLike",
				new SuffixModelTemplate("JustLike", "JustLike", "Andırma (imsi)", SuffixType.DERIVATIONAL));
		suffixMap.put("A1pl", new SuffixModelTemplate("A1pl", "FirstPersonPlural", "Birinci Çoğul Şahıs Eki (biz)",
				SuffixType.INFLEXIONAL));
		suffixMap.put("PastPart",
				new SuffixModelTemplate("PastPart", "PastParticiple", "Sıfat-Fiil Eki (dık)", SuffixType.DERIVATIONAL));
		suffixMap.put("Dup", new SuffixModelTemplate("Dup", "Duplicator", "İkileme", SuffixType.INFLEXIONAL));
		suffixMap.put("A2pl", new SuffixModelTemplate("A2pl", "SecondPersonPlural", "İkinci Çoğul Şahıs Eki (siz)",
				SuffixType.INFLEXIONAL));
		suffixMap.put("Cond", new SuffixModelTemplate("Cond", "Condition", "Koşul (sa, se)", SuffixType.DERIVATIONAL));
		suffixMap.put("Recip",
				new SuffixModelTemplate("Recip", "Reciprocal", "İşteşlik (ış, iş)", SuffixType.DERIVATIONAL));
		suffixMap.put("Dim",
				new SuffixModelTemplate("Dim", "Diminutive", "Küçültme (cık, cağız)", SuffixType.DERIVATIONAL));
		suffixMap.put("Cop",
				new SuffixModelTemplate("Cop", "Copula", "Ek fiil (ydı, ymış, ysa, yken)", SuffixType.INFLEXIONAL));
		suffixMap.put("Narr", new SuffixModelTemplate("Narr", "NarrativeTense", "Öğrenilen Geçmiş Zaman (miş)",
				SuffixType.INFLEXIONAL));
		suffixMap.put("NarrPart", new SuffixModelTemplate("NarrPart", "NarrativeParticiple",
				"Sıfat-Fiil Eki (mış, miş, muş, müş)", SuffixType.DERIVATIONAL));
		suffixMap.put("AfterDoingSo", new SuffixModelTemplate("AfterDoingSo", "AfterDoingSo", "Zarf-Fiil Eki (ip, ıp)",
				SuffixType.DERIVATIONAL));
		suffixMap.put("Unable",
				new SuffixModelTemplate("Unable", "Unable", "Yetersizlik (ama, eme)", SuffixType.INFLEXIONAL));
		suffixMap.put("FeelLike", new SuffixModelTemplate("FeelLike", "FeelLike",
				"İstek Belirten Sıfat-fiil Eki (ası, esi)", SuffixType.DERIVATIONAL));
		suffixMap.put("Desr",
				new SuffixModelTemplate("Desr", "Desire", "İstek Kipi (sa, se)", SuffixType.DERIVATIONAL));
		suffixMap.put("Neces",
				new SuffixModelTemplate("Neces", "Necessity", "Gereklilik Kipi (meli, malı)", SuffixType.INFLEXIONAL));
		suffixMap.put("FutPart", new SuffixModelTemplate("FutPart", "FutureParticiple", "Sıfat-Fiil Eki (acak, ecek)",
				SuffixType.DERIVATIONAL));
		suffixMap.put("NotState", new SuffixModelTemplate("NotState", "NotState",
				"Eylemi Gerçekleştirmeme (mezlik, mazlık)", SuffixType.DERIVATIONAL));
		suffixMap.put("While",
				new SuffixModelTemplate("While", "While", "Zarf-Fiil Eki (yken)", SuffixType.INFLEXIONAL));
		suffixMap.put("Rel", new SuffixModelTemplate("Rel", "Relation", "İlgi Zamiri (ki)", SuffixType.DERIVATIONAL));
		suffixMap.put("P2pl", new SuffixModelTemplate("P2pl", "SecondPersonPluralPossessive",
				"İkinci Çoğul Şahıs İyelik Eki (sizin)", SuffixType.INFLEXIONAL));
		suffixMap.put("SinceDoingSo", new SuffixModelTemplate("SinceDoingSo", "SinceDoingSo",
				"Zarf-Fiil Eki (eli, alı)", SuffixType.INFLEXIONAL));
		suffixMap.put("When",
				new SuffixModelTemplate("When", "When", "Zarf-Fiil Eki (ince, ınca)", SuffixType.INFLEXIONAL));
		suffixMap.put("Fut",
				new SuffixModelTemplate("Fut", "Future", "Gelecek Zaman (acak, ecek)", SuffixType.INFLEXIONAL));
		suffixMap.put("P1pl", new SuffixModelTemplate("P1pl", "FirstPersonPluralPossessive",
				"Birinci Çoğul Şahıs İyelik Eki (bizim)", SuffixType.INFLEXIONAL));
		suffixMap.put("ActOf", new SuffixModelTemplate("ActOf", "ActOf", "Fiilden İsim Yapma Eki (maca, mece)",
				SuffixType.DERIVATIONAL));
		suffixMap.put("WithoutHavingDoneSo", new SuffixModelTemplate("WithoutHavingDoneSo", "WithoutHavingDoneSo",
				"Zarf-Fiil Eki (madan, meden)", SuffixType.INFLEXIONAL));
		suffixMap.put("Almost",
				new SuffixModelTemplate("Almost", "Almost", "Yaklaşma Eki (ayaz)", SuffixType.DERIVATIONAL));
		suffixMap.put("Ques",
				new SuffixModelTemplate("Ques", "Question", "Soru Eki (mi, mı, mu, mü)", SuffixType.INFLEXIONAL));
		suffixMap.put("Reflex",
				new SuffixModelTemplate("Reflex", "Reflexive", "Dönüşlülük ((i)n)", SuffixType.DERIVATIONAL));
		suffixMap.put("EverSince", new SuffixModelTemplate("EverSince", "EverSince", "Sürerlik Eki/Devam (egel, agel)",
				SuffixType.DERIVATIONAL));
		suffixMap.put("Stay",
				new SuffixModelTemplate("Stay", "Stay", "Sürerlik Eki/Duran (ekal, akal)", SuffixType.DERIVATIONAL));
		suffixMap.put("Repeat", new SuffixModelTemplate("Repeat", "Repeat", "Sürerlik Eki/Tekrar (edur, adur)",
				SuffixType.DERIVATIONAL));
		suffixMap.put("Punc",
				new SuffixModelTemplate("Punc", "Punctuation", "Noktalama İşareti", SuffixType.INFLEXIONAL));
		suffixMap.put("Prog1",
				new SuffixModelTemplate("Prog1", "Progressive1", "Şimdiki Zaman (iyor)", SuffixType.INFLEXIONAL));
		suffixMap.put("ByDoingSo", new SuffixModelTemplate("ByDoingSo", "ByDoingSo", "Zarf-Fiil Eki (arak, erek)",
				SuffixType.INFLEXIONAL));
		suffixMap.put("Adamantly", new SuffixModelTemplate("Adamantly", "Adamantly", "Zarf-Fiil Eki (asıya, esiye)",
				SuffixType.DERIVATIONAL));
		suffixMap.put("AsLongAs", new SuffixModelTemplate("AsLongAs", "AsLongAs", "Zarf-Fiil Eki (dıkça, dikçe)",
				SuffixType.INFLEXIONAL));
		suffixMap.put("Start", new SuffixModelTemplate("Start", "Start", "Sürerlik Eki/Başlama (ekoy, akoy)",
				SuffixType.DERIVATIONAL));

	}

	public static SuffixModelTemplate getSuffixModel(String tmpModel) {
		if (suffixMap == null) {
			Initialize();
		}
		return suffixMap.get(tmpModel);
	}
}
