require("dotenv").config();
const axios = require("axios");

const {
    CF_API_TOKEN,
    CF_ZONE_ID,
    CF_DOMAIN
} = process.env;

const headers = {
    Authorization: `Bearer ${CF_API_TOKEN}`,
    "Content-Type": "application/json"
};

async function getPublicIp() {
    const res = await axios.get("https://api.ipify.org?format=json");
    return res.data.ip;
}

async function getDnsRecord(domain) {
    const url = `https://api.cloudflare.com/client/v4/zones/${CF_ZONE_ID}/dns_records`;

    const params = {
        type: "A",
        name: domain
    };

    try {
        const res = await axios.get(url, { headers, params });
        if (res.data.success && res.data.result.length > 0) {
            return res.data.result[0];
        } else {
            throw new Error(`DNS-Record nicht gefunden für ${domain}`);
        }
    } catch (error) {
        if (error.response) {
            throw new Error(
                `Cloudflare API Fehler: ${error.response.status} - ${JSON.stringify(error.response.data)}`
            );
        }
        throw error;
    }
}

async function updateDnsRecord(recordId, name, ip) {
    const url = `https://api.cloudflare.com/client/v4/zones/${CF_ZONE_ID}/dns_records/${recordId}`;

    const body = {
        type: "A",
        name,
        content: ip,
        ttl: 1,
        proxied: true
    };

    const res = await axios.put(url, body, { headers });
    return res.data;
}

(async () => {
    try {
        const ip = await getPublicIp();
        console.log("🌐 Öffentliche IP:", ip);

        const record = await getDnsRecord(CF_DOMAIN);
        console.log("📄 Aktueller DNS-Record:", record.content);

        const wildcardDomain = `*.${CF_DOMAIN}`;
        const record2 = await getDnsRecord(wildcardDomain);
        console.log("📄 Aktueller Wildcard-DNS-Record:", record2.content);

        if (record.content !== ip || record2.content !== ip) {
            console.log("🔄 IP hat sich geändert. Aktualisiere...");
            const result = await updateDnsRecord(record.id, CF_DOMAIN, ip);
            const result2 = await updateDnsRecord(record2.id, wildcardDomain, ip);

            if (result.success && result2.success) {
                console.log("✅ DNS-Einträge aktualisiert.");
            } else {
                console.error("❌ Fehler bei Update:", result.errors || result2.errors);
            }
        } else {
            console.log("✅ IP ist aktuell. Kein Update nötig.");
        }
    } catch (err) {
        console.error("❌ Fehler:", err.message);
    }
})();
