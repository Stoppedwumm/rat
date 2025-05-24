import org.bouncycastle.openpgp.*;
import org.bouncycastle.openpgp.jcajce.JcaPGPObjectFactory;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentVerifierBuilderProvider;
import org.bouncycastle.bcpg.ArmoredInputStream;

import java.io.*;
import java.security.Security;
import java.util.Iterator;

public class SignatureVerifier {
    public static void main(String[] args) throws Exception {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

        // Load public key
        InputStream publicKeyIn = new FileInputStream("public.key");
        PGPPublicKeyRingCollection pubKeyRingCollection = new PGPPublicKeyRingCollection(
                PGPUtil.getDecoderStream(publicKeyIn),
                new JcaKeyFingerprintCalculator()
        );

        // Load the file and its detached signature
        byte[] fileBytes = new FileInputStream("my-rat.jar").readAllBytes();
        InputStream sigIn = new ArmoredInputStream(new FileInputStream("my-rat.jar.asc"));
        PGPObjectFactory pgpFactory = new JcaPGPObjectFactory(sigIn);
        PGPSignatureList sigList = (PGPSignatureList) pgpFactory.nextObject();
        PGPSignature signature = sigList.get(0);

        // Find the matching public key
        PGPPublicKey pubKey = null;
        Iterator<PGPPublicKeyRing> ringIter = pubKeyRingCollection.getKeyRings();
        while (ringIter.hasNext() && pubKey == null) {
            PGPPublicKeyRing ring = ringIter.next();
            pubKey = ring.getPublicKey(signature.getKeyID());
        }

        if (pubKey == null) {
            throw new IllegalArgumentException("No matching public key found.");
        }

        signature.init(new JcaPGPContentVerifierBuilderProvider().setProvider("BC"), pubKey);
        signature.update(fileBytes);

        if (signature.verify()) {
            System.out.println("✅ Signature is valid!");
        } else {
            System.out.println("❌ Signature verification failed!");
        }
    }
}
