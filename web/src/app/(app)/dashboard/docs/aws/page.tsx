import {
    GuideShell,
    GuideHeader,
    Step,
    ExternalLink,
    Code,
    Strong,
    Warning,
    Note,
    Troubleshooting,
} from "../_components/guide";
import {
    CreateUserDiagram,
    PolicyDiagram,
    AccessKeyDiagram,
    ConnectDiagram,
} from "./diagrams";

export const metadata = {
    title: "How to connect AWS",
};

export default function AwsSetupGuide() {
    return (
        <GuideShell>
            <GuideHeader
                logo="/integrations/aws.webp"
                logoAlt="AWS logo"
                title="How to connect AWS"
                prerequisites={{
                    label: "Before you start",
                    items: [
                        <>
                            An AWS account where you can create IAM users — if you&apos;re on a
                            company account, this may need an administrator.
                        </>,
                        <>
                            At least one S3 bucket with objects in it, or every metric will read
                            zero.
                        </>,
                    ],
                }}
            >
                <p>
                    Threadspace reads your S3 storage through an{" "}
                    <Strong>IAM user</Strong> — a dedicated account with its own access keys,
                    separate from your own login. You&apos;ll create one, give it read access to
                    S3, generate a key pair, and paste it into Threadspace. Takes about five
                    minutes.
                </p>
            </GuideHeader>

            <div className="space-y-12">
                <Step n={1} title="Create an IAM user" diagram={<CreateUserDiagram />}>
                    <p>
                        Open the{" "}
                        <ExternalLink href="https://console.aws.amazon.com/iam/home#/users">
                            IAM console → Users
                        </ExternalLink>{" "}
                        and click <Strong>Create user</Strong>.
                    </p>
                    <p>
                        Name it something you&apos;ll recognise later, like{" "}
                        <Code>threadspace-integration</Code>. Leave{" "}
                        <Strong>Provide user access to the AWS Management Console</Strong>{" "}
                        unchecked — this user never signs in to a browser, it only needs API
                        keys.
                    </p>
                    <Note>
                        Use a dedicated user rather than your own credentials. It can be revoked
                        on its own without disrupting anything else you do in AWS, and its
                        permissions stay scoped to exactly what Threadspace needs.
                    </Note>
                </Step>

                <Step n={2} title="Attach S3 read access" diagram={<PolicyDiagram />}>
                    <p>
                        On the permissions step, choose{" "}
                        <Strong>Attach policies directly</Strong>, search for{" "}
                        <Code>s3 read</Code>, and tick{" "}
                        <Strong>AmazonS3ReadOnlyAccess</Strong>.
                    </p>
                    <Warning>
                        Don&apos;t skip this step. A new IAM user has{" "}
                        <Strong>no permissions at all</Strong>, so the credentials will verify
                        successfully and then fail the moment Threadspace tries to list your
                        buckets. This is the single most common cause of a failed AWS connect.
                    </Warning>
                    <p>
                        Threadspace calls two S3 operations:{" "}
                        <Code>s3:ListAllMyBuckets</Code> to enumerate your buckets and{" "}
                        <Code>s3:ListBucket</Code> to count the objects inside each one.{" "}
                        <Strong>AmazonS3ReadOnlyAccess</Strong> covers both. If your
                        organisation requires tighter scoping, a custom policy granting just
                        those two actions is enough — Threadspace never reads object contents.
                    </p>
                </Step>

                <Step n={3} title="Create an access key" diagram={<AccessKeyDiagram />}>
                    <p>
                        Click into the new user, open the{" "}
                        <Strong>Security credentials</Strong> tab, and choose{" "}
                        <Strong>Create access key</Strong>. Pick{" "}
                        <Strong>Third-party service</Strong> as the use case and confirm.
                    </p>
                    <p>
                        You&apos;ll get an <Strong>Access key ID</Strong> starting with{" "}
                        <Code>AKIA</Code> and a <Strong>Secret access key</Strong>.
                    </p>
                    <Note>
                        The secret is displayed <Strong>once</Strong>, on this screen only. Copy
                        it or download the <Code>.csv</Code> before navigating away — if you lose
                        it, delete the key and create another. Treat it like a password, and
                        never commit it to git.
                    </Note>
                </Step>

                <Step n={4} title="Hand it to Threadspace" diagram={<ConnectDiagram />}>
                    <p>
                        Back on the Connect AWS screen, paste the two values and pick a{" "}
                        <Strong>Region</Strong>.
                    </p>
                    <p>
                        S3 bucket names are global, so the region doesn&apos;t limit which
                        buckets you see — it only decides which endpoint Threadspace talks to.
                        Pick the one closest to where your buckets live.
                    </p>
                    <p>
                        Threadspace verifies the key pair against AWS before saving, so if the
                        form accepts it, the credentials are genuinely valid.
                    </p>
                </Step>
            </div>

            <Troubleshooting
                entries={[
                    {
                        q: "An access denied or 403 error",
                        a: (
                            <>
                                The IAM user has no S3 permissions. Go back to the user in IAM,
                                open <Strong>Permissions</Strong>, and attach{" "}
                                <Strong>AmazonS3ReadOnlyAccess</Strong>. This is by far the most
                                common failure.
                            </>
                        ),
                    },
                    {
                        q: "InvalidClientTokenId or SignatureDoesNotMatch",
                        a: (
                            <>
                                The key pair is wrong or mistyped — often a truncated secret, or
                                trailing whitespace from the copy. Generate a fresh access key and
                                paste both values again.
                            </>
                        ),
                    },
                    {
                        q: "It worked before and now it doesn't",
                        a: "The access key was probably deactivated or deleted in IAM, or a permissions boundary changed. Check the key is still Active on the user's Security credentials tab.",
                    },
                    {
                        q: "It connects, but everything shows zero",
                        a: "That's not an error. The credentials worked and the account simply has no buckets, or the buckets are empty. Create a bucket and upload a few files.",
                    },
                ]}
            />
        </GuideShell>
    );
}
