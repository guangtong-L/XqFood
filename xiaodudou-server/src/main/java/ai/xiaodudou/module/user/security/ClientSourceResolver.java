package ai.xiaodudou.module.user.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;

/**
 * 登录来源解析。默认只信任容器看到的 remoteAddr；只有请求确实来自显式配置的可信代理网段时，
 * 才读取 X-Forwarded-For 的首个地址，防止客户端伪造来源绕过限流。
 */
@Component
public class ClientSourceResolver {

    private final List<IpNetwork> trustedProxies;

    public ClientSourceResolver(@Value("${xiaodudou.security.trusted-proxies:}") String configuredNetworks) {
        this.trustedProxies = Arrays.stream(configuredNetworks.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(IpNetwork::parse)
                .toList();
    }

    public String resolve(HttpServletRequest request) {
        String remote = normalize(request.getRemoteAddr());
        if (remote == null || trustedProxies.stream().noneMatch(network -> network.contains(remote))) {
            return remote == null ? "unknown" : remote;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded == null || forwarded.length() > 512) return remote;
        String first = forwarded.split(",", 2)[0].trim();
        String normalized = normalize(first);
        return normalized == null ? remote : normalized;
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank() || value.contains("%")) return null;
        String candidate = value.trim();
        // 仅接受 IP 字面量，禁止对不可信请求头做 DNS 解析。
        if (!isIpLiteral(candidate)) return null;
        try {
            return InetAddress.getByName(candidate).getHostAddress();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static boolean isIpLiteral(String value) {
        if (value.indexOf(':') >= 0) {
            return value.matches("[0-9A-Fa-f:.]+") && value.chars().filter(ch -> ch == ':').count() >= 2;
        }
        if (!value.matches("[0-9.]+")) return false;
        String[] octets = value.split("\\.", -1);
        if (octets.length != 4) return false;
        for (String octet : octets) {
            if (octet.isEmpty() || octet.length() > 3) return false;
            int number = Integer.parseInt(octet);
            if (number > 255) return false;
        }
        return true;
    }

    private record IpNetwork(byte[] address, int prefixLength) {
        static IpNetwork parse(String value) {
            try {
                String[] parts = value.split("/", 2);
                if (!isIpLiteral(parts[0])) throw new IllegalArgumentException();
                byte[] address = InetAddress.getByName(parts[0]).getAddress();
                int bits = address.length * 8;
                int prefix = parts.length == 2 ? Integer.parseInt(parts[1]) : bits;
                if (prefix < 0 || prefix > bits) throw new IllegalArgumentException();
                return new IpNetwork(address, prefix);
            } catch (Exception e) {
                throw new IllegalArgumentException("可信代理地址配置无效", e);
            }
        }

        boolean contains(String candidate) {
            try {
                byte[] other = InetAddress.getByName(candidate).getAddress();
                if (other.length != address.length) return false;
                int wholeBytes = prefixLength / 8;
                int remainingBits = prefixLength % 8;
                for (int i = 0; i < wholeBytes; i++) {
                    if (address[i] != other[i]) return false;
                }
                if (remainingBits == 0) return true;
                int mask = 0xFF << (8 - remainingBits);
                return (address[wholeBytes] & mask) == (other[wholeBytes] & mask);
            } catch (Exception e) {
                return false;
            }
        }
    }
}
